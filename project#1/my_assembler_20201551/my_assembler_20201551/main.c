/*
 * 파일명 : my_assembler_00000000.c
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 * 파일 내에서 사용되는 문자열 "00000000"에는 자신의 학번을 기입한다.
 */

/*
 *
 * 프로그램의 헤더를 정의한다.
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <ctype.h>

// 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20201551.h"

/* ----------------------------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일
 * 반환 : 성공 = 0, 실패 = < 0
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다.
 *         또한 중간파일을 생성하지 않는다.
 * ----------------------------------------------------------------------------------
 */
int main(int args, char *arg[])
{
   if (init_my_assembler() < 0)
   {
      printf("init_my_assembler: 프로그램 초기화에 실패 했습니다.\n");
      return -1;
   }

   if (assem_pass1() < 0)
   {
      printf("assem_pass1: 패스1 과정에서 실패하였습니다.  \n");
      return -1;
   }


   make_symtab_output("output_symtab.txt");
   make_literaltab_output("output_littab.txt");

   if (assem_pass2() < 0)
   {
      printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
      return -1;
   }

    make_objectcode_output(NULL);
   
   return 0;
}
// 개별로 만든 함수
// 토큰에 개행 문자를 모두 삭제하고 테이블에 넣도록 하기 위해 만듦
void trim(char* s) {
    s[strcspn(s, "\n\r\t ")] = '\0';  // 앞에서 처음 나오는 특수문자 위치를 '\0' 처리
}

// 문자열에서 쉼표로 구분된 토큰을 추출해 배열에 저장
int sep_by_comma(char* line, char* output[], int max_symbols) {
    char buffer[256];
    strncpy(buffer, line, sizeof(buffer));
    buffer[sizeof(buffer) - 1] = '\0';

    char* token = strtok(buffer, ",");
    int count = 0;

    while (token != NULL && count < max_symbols) {
        while (isspace((unsigned char)*token)) token++; // 앞 공백 제거

        output[count] = (char*)malloc(strlen(token) + 1);
        strcpy(output[count], token);
        count++;

        token = strtok(NULL, ",");
    }

    return count; // 실제 저장된 심볼 수
}

/* ----------------------------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다.
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기
 *         위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *         구현하였다.
 * ----------------------------------------------------------------------------------
 */
int init_my_assembler(void)
{
   int result;

   if ((result = init_inst_file("inst_table.txt")) < 0)
      return -1;
   if ((result = init_input_file("input-1.txt")) < 0)
      return -1;
   return result;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일(inst_table.txt)을 읽어
 *       기계어 목록 테이블(inst_table)을 생성하는 함수이다.
 *
 *
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 예시는 다음과 같다.
 *
 *   ===============================================================================
 *         | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | \n |
 *   ===============================================================================
 *
 * ----------------------------------------------------------------------------------
 */
int init_inst_file(char *inst_file)
{
    FILE* file;
    
    char mnemonic[10];
    int format;
    unsigned int opcode;
    int ops;
    
    inst_index = 0;
    
    file = fopen(inst_file, "r");
    if(file == NULL){
        perror("inst_table.txt 파일 열기 실패");
        return -1;
    }
    
    while(fscanf(file, "%s %d %x %d", mnemonic, &format, &opcode, &ops) == 4){
        if(inst_index >= MAX_INST){
            printf("inst_table의 최대 크기를 초과하였습니다.\n");
            break;
        }
        
        inst* new_inst = (inst*)malloc(sizeof(inst));
        if(new_inst == NULL){
            perror("inst 구조체 메모리 할당 실패");
            fclose(file);
            return -1;
        }
        
        strcpy(new_inst->str, mnemonic);
        new_inst->format = format;
        new_inst->op = (unsigned char)opcode;
        new_inst->ops = ops;
        
        inst_table[inst_index++] = new_inst;
    }
    
    fclose(file);
    return 0;  // 정상 종료}
}
/* ----------------------------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드를 읽어 소스코드 테이블(input_data)를 생성하는 함수이다.
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 라인단위로 저장한다.
 *
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char *input_file)
{
    FILE* file;

    /* add your code here */
    char buffer[256];
    line_num = 0;
    
    file = fopen(input_file, "r");
    if (file == NULL)
    {
        perror("파일 열기 실패");
        return -1;
    }
    
    while(fgets(buffer, sizeof(buffer), file)) // 한 줄씩
    {
        if(line_num >= MAX_LINES){
            printf("입력 라인이 MAX_LINES를 초과하였습니다.\n");
            break;
        }
        
        buffer[strcspn(buffer, "\n")] = '\0'; //strcspn으로 "\n" 인덱스 반환 후 "\n" -> '\0'
        
        input_data[line_num] = (char*)malloc(strlen(buffer) + 1);
        if(input_data[line_num] == NULL)
        {
            perror("메모리 할당 실패");
            fclose(file);
            return -1;
        }
        
        strcpy(input_data[line_num++], buffer);

    }
    
    fclose(file);

    return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다.
 *        패스 1로 부터 호출된다.
 * 매계 : 파싱을 원하는 문자열
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다.
 * ----------------------------------------------------------------------------------
 */
int is_in_directive_list(const char* value) {
    const char* direct_list[] = {"LTORG", "EXTDEF", "EXTREF"};
    int list_size = sizeof(direct_list) / sizeof(direct_list[0]);

    for (int i = 0; i < list_size; i++) {
        if (strcmp(value, direct_list[i]) == 0) {
            return 0;  // 있음
        }
    }
    return -1;  // 없음
}

int token_parsing(char *str)
{
    if (token_line >= MAX_LINES) {
        printf("token_table 크기 초과\n");
        return -1;
    }

    token* t = (token*)malloc(sizeof(token));
    if (!t) {
        perror("토큰 메모리 할당 실패");
        return -1;
    }

    for (int i = 0; i < MAX_OPERAND; i++)
        t->operand[i] = NULL;

    t->label = NULL;
    t->operator = NULL;
    strcpy(t->comment, "");

    
    char* tok_list[MAX_COLUMNS + 1] = { NULL, };
    int count = 0;
    
    // 토큰 분리 (탭)
    char* tok = strtok(str, "\t");
    while (tok != NULL && count < MAX_COLUMNS + 1) {
        if (tok[0] == '.') {
            break;
        }
        tok[strcspn(tok,"\n")] = '\0';
        tok_list[count++] = tok;
        tok = strtok(NULL, " \t");
    }

    if (count == 0) {
        token_table[token_line++] = t;
        return 0;
    }
    
    // 집어넣기 전에 개행 먼저 삭제
    for(int i = 0; i < count; i++){
        trim(tok_list[i]);
    }

    // 첫 토큰이 opcode인 경우 → label 없음
    int opcode_idx = search_opcode(tok_list[0]);
    int direct_idx = is_in_directive_list(tok_list[0]);
    int is_label = (opcode_idx < 0 && direct_idx < 0); // 명령어가 아니면 label 취급
    
    
    
    
    if (is_label) {
        t->label = strdup(tok_list[0]);
        if (count > 1) t->operator = strdup(tok_list[1]);
        if (count > 2) t->operand[0] = strdup(tok_list[2]);
        if (count > 3) t->operand[1] = strdup(tok_list[3]);
    } else {
        t->label = NULL;
        if (count > 0) t->operator = strdup(tok_list[0]);
        if (count > 1) t->operand[0] = strdup(tok_list[1]);
        if (count > 2) t->operand[1] = strdup(tok_list[2]);
    }

    token_table[token_line++] = t;
    return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다.
 * 매계 : 토큰 단위로 구분된 문자열
 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0
 * 주의 : 기계어 목록 테이블에서 특정 기계어를 검색하여, 해당 기계어가 위치한 인덱스를 반환한다.
 *        '+JSUB'과 같은 문자열에 대한 처리는 자유롭게 처리한다.
 *
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char *str)
{
   /* add your code here */
    char temp[10];

    // format 4 명령어(+기호 제거)
    if (str[0] == '+') {
        strncpy(temp, str + 1, sizeof(temp) - 1);
        temp[sizeof(temp) - 1] = '\0';
    } else {
        strncpy(temp, str, sizeof(temp) - 1);
        temp[sizeof(temp) - 1] = '\0';
    }

    for (int i = 0; i < inst_index; i++) {
        if (strcmp(inst_table[i]->str, temp) == 0) {
            return i;
        }
    }

    return -1;  // 명령어를 찾지 못한 경우
}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*         패스1에서는..
*         1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*         테이블을 생성한다.
*          2. 토큰 테이블은 token_parsing()을 호출하여 설정한다.
*          3. assem_pass2 과정에서 사용하기 위한 심볼테이블 및 리터럴 테이블을 생성한다.
*
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*     따라서 에러에 대한 검사 루틴을 추가해야 한다.
*
* -----------------------------------------------------------------------------------
*/
// extref에 있는지 확인하는 함수
int is_in_extref_list(const char* value) {
    for (int i = 0; i < extref_index; i++) {
        if (strcmp(value, extref_table[i]) == 0) {
            return 0;  // 있음
        }
    }
    return -1;  // 없음
}
// extref_2에 있는지 확인하는 함수
int is_in_extref_2_list(const char* value) {
    for (int i = 0; i < extref_2_index; i++) {
        if (strcmp(value, extref_2_table[i]) == 0) {
            return 0;  // 있음
        }
    }
    return -1;  // 없음
}
// extdef에 있는지 확인하는 함수
int is_in_extdef_list(const char* value){
    for(int i = 0; i < MAX_OPERAND; i++) {
        if(strcmp(value, extdef_table[i]) == 0){
            return 0; // 있음
        }
    }
    return -1; // 없음
}
// EQU에서 피연산자를 추출하는 함수
int extract_operands(char* expr, char* operands[2]){
    const char* pos = strpbrk(expr, "+-*/");
    
    if(!pos){
        // 연산자가 없으니 operand가 하나
        operands[0] = strdup(expr);
        operands[1] = strdup(NULL);
        return 1;
    }
    
    // 연산자가 있으면 operand가 2개 이상(여기선 최대 2개라고 가정)
    num_operator = *pos;
    long len_left = pos - expr;
    operands[0] = (char*)malloc(len_left+1);
    strncpy(operands[0], expr, len_left);
    operands[0][len_left] = '\0';
    
    operands[1] = strdup(pos + 1);
    return 2;
}
// 문자열이 숫자인지 아닌지 확인하는 함수
int is_numeric(char* str) {
    if (str == NULL || *str == '\0') return 0;  // 빈 문자열 또는 NULL은 숫자 아님

    // 음수 부호 '-' 체크 (선택사항)
    if (*str == '-') str++;  // 첫 문자가 '-'일 경우 skip

    while (*str) {
        if (!isdigit((unsigned char)*str))
            return 0;  // 숫자가 아니면 false
        str++;
    }

    return 1;  // 모두 숫자면 true
}
//
void update_ext_tables_on_directives(token* tk) {
    if (strcmp(tk->operator, "EXTDEF") == 0) {
        for (int i = 0; i < MAX_OPERAND; i++) extdef_table[i] = NULL;
        extdef_index = sep_by_comma(tk->operand[0], extdef_table, MAX_OPERAND);
    }
    if (strcmp(tk->operator, "EXTREF") == 0) {
        for (int i = 0; i < MAX_OPERAND; i++) extref_table[i] = NULL;
        extref_index = sep_by_comma(tk->operand[0], extref_table, MAX_OPERAND);
    }
}
//----------------------------------------------------------//
static int assem_pass1(void)
{
   /* add your code here */

   /* input_data의 문자열을 한줄씩 입력 받아서
    * token_parsing()을 호출하여 _token에 저장
    */
    literal_index = 0;
    sym_index = 0;// 리터럴 테이블에 리터럴이 몇 개 들어있는지 확인하기 위함
    extdef_index = 0;
    extref_index = 0;
    int ltorg_check = 0; // 리터럴 테이블에서 어디까지 주소가 배정되었는지 확인하기 위함
    token_line = 0;
    locctr = 0;

    for (int i = 0; i < line_num; i++) {
        if (input_data[i] == NULL || input_data[i][0] == '\0') continue;
        if (input_data[i][0] == '.') continue;
        if (token_parsing(input_data[i]) < 0) {
            fprintf(stderr, "Token parsing failed at line %d\n", i);
            return -1;
        }
        token* tk = token_table[token_line - 1];
        update_ext_tables_on_directives(tk);
        
        // 여기서 locctr이 Control Section을 만나면 0으로 초기화 된다.
        if((strcmp(tk->operator, "CSECT")) == 0){
            locctr = 0;}
        if(strcmp(tk->operator, "EXTDEF") == 0){
            // 피연산자 추출 후 extdef_table에 넣음
            extdef_index = sep_by_comma(tk->operand[0], extdef_table, 3);
        }
        if(strcmp(tk->operator, "EXTREF") == 0){
            // 피연산자 추출 후 extref_table에 넣음
            extref_index = sep_by_comma(tk->operand[0], extref_table, 3);
        }
        
        // 심볼 테이블에 추가
        if (tk->label) {
            strcpy(sym_table[sym_index].symbol, tk->label);
            if(strcmp(tk->operator, "EQU") == 0){ //EQU 발견시
                if(strcmp(tk->operand[0], "*") == 0){
                    sym_table[sym_index++].addr = locctr;
                }
                else {
                    // 피연산자 extract
                    char* operands[2];
                    int operands_each = 0;
                    operands_each = extract_operands(tk->operand[0], operands);
                    
                    // 피연산자 extract 후 피연산자의 정보가 extref_table에 있는지 없는지 확인
                    // 있다면 00000으로 채우고 없다면 symtable을 보고 넣어야 함
                    int count_isin = 0;
                    for(int i = 0; i < operands_each; i++){
                        if(is_in_extref_list(operands[i]) == 0)
                            count_isin += 1;
                    }
                    if(count_isin > 0){
                        sym_table[sym_index++].addr = 00000;
                    }
                    else{
                        int temp_list[operands_each];
                        int b = 0;
                        // temp_list에 operands의 값을 각각 넣는다. 왜냐하면 symtable에 있다는 뜻은 계산해서 넣을 수 있다는 것이니까!
                        for(int i = 0; i < sym_index; i++){
                            for(int j = 0; j < operands_each; j++){
                                if(strcmp(sym_table[i].symbol, operands[j]) == 0){
                                    temp_list[b++] = sym_table[i].addr;
                                }
                            }
                        }
                        // temp_list가 완성되면 num_operator에 들어있는 것을 활용하여 계산하면 됨
                        
                        if(num_operator){
                            int sum = 0;
                            switch (num_operator) {
                                case '+':
                                    sum = temp_list[b - 1] + temp_list[b - 2];
                                    break;
                                case '-':
                                    sum = temp_list[b - 1] - temp_list[b - 2];
                                    break;
                                default:
                                    break;
                            }
                            printf("%04X", sum);
                            
                            sym_table[sym_index++].addr = sum;
                        }
                        else
                            sym_table[sym_index++].addr = temp_list[0];
                    }
                    
                }
            }
            else
                sym_table[sym_index++].addr = locctr;
        }

            // 리터럴 발견 시 리터럴 테이블에 등록
        for (int j = 0; j < MAX_OPERAND; j++) {
            if (tk->operand[j] && tk->operand[j][0] == '=') {
                int exists = 0;
                for (int k = 0; k < literal_index; k++) {
                    if (strcmp(literal_table[k].literal, tk->operand[j]) == 0) { // strcmp --> 비교하하는 것
                        exists = 1;
                        break;
                    }
                }
                if (!exists && literal_index < MAX_LINES) {
                    literal_table[literal_index].literal = strdup(tk->operand[j]); // strdup은 복사하는 것
                    literal_table[literal_index].addr = -1; // 주소는 LTORG나 END에서 할당
                    literal_index++;
                }
            }
        }
        // 디버깅과 중간 점검을 위해 배치
        printf("%s, %s, %s, %s \n", tk->label, tk->operator, tk->operand[0], tk->operand[1]);
        tk->loc = locctr;

        int idx = search_opcode(tk->operator);
        if (idx >= 0) {
            if(tk->operator[0] == '+')
                locctr += 4;
            else
                locctr += inst_table[idx]->format;
        } else {
            if((strcmp("LTORG", tk->operator)) == 0){ // LTORG를 만났을 때 -> locctr을 Literal에 부여할 수가 있다.
                for (int i = 0; i < literal_index; i++) {
                    if (literal_table[i].addr == -1) {
                        literal_table[i].addr = locctr;
                        if (literal_table[i].literal[1] == 'C') {
                            locctr += strlen(literal_table[i].literal) - 4;
                        } else {
                            locctr += (strlen(literal_table[i].literal) - 4) / 2;
                        }
                    }
                }
                ltorg_check = literal_index;
            }
            if((strcmp("RESB", tk->operator)) == 0){
                int add_1 = atoi(tk->operand[0]);
                locctr += add_1;
            }
            if((strcmp("RESW", tk->operator)) == 0){
                int add_2 = atoi(tk->operand[0]) * 3;
                locctr += add_2;
            }
            if((strcmp("BYTE", tk->operator)) == 0){
                long add_3 = (strlen(tk->operand[0]) - 3) / 2;
                locctr += add_3;
            }
            if((strcmp("WORD", tk->operator)) == 0){
                if(is_numeric(tk->operand[0]) == 0){
                    char* operands[2];
                    int operands_each = 0;
                    operands_each = extract_operands(tk->operand[0], operands);
                    
                    int temp_list[operands_each];
                    int b = 0;
                    // temp_list에 operands의 값을 각각 넣는다. 왜냐하면 symtable에 있다는 뜻은 계산해서 넣을 수 있다는 것이니까!
                    for(int i = 0; i < sym_index; i++){
                        for(int j = 0; j < operands_each; j++){
                            if(strcmp(sym_table[i].symbol, operands[j]) == 0){
                                temp_list[b++] = sym_table[i].addr;
                            }
                        }
                    }
                    // temp_list가 완성되면 num_operator에 들어있는 것을 활용하여 계산하면 됨
                    
                    if(num_operator){
                        int sum = 0;
                        switch (num_operator) {
                            case '+':
                                sum = temp_list[b - 1] + temp_list[b - 2];
                                break;
                            case '-':
                                sum = temp_list[b - 1] - temp_list[b - 2];
                                break;
                            default:
                                break;
                        }
                        printf("%04X", sum);
                        locctr += sum*3;
                    }
                    locctr += temp_list[0]*3;
                }
                
            }
        }
    }

        // END 시점에 리터럴 주소 할당
    for (int i = 0; i < literal_index; i++) {
        if (literal_table[i].addr == -1) {
            literal_table[i].addr = locctr;
            if(literal_table[ltorg_check - 1].literal[1] == 'C'){
                locctr += (strlen(literal_table[ltorg_check - 1].literal) - 4); // C가 나왔을 때 주소 부여
            }
            else{
                locctr += (strlen(literal_table[ltorg_check - 1].literal) - 4) / 2; // X가 나왔을 때 주소 부여
            }
        }
    }
    
    
    
    // print
    // symtab
    for(int i = 0; i < sym_index; i++) {
        if(strcmp(sym_table[i].symbol, "RDREC") == 0 || strcmp(sym_table[i].symbol, "WRREC") == 0)
            printf("\n");
        if(strcmp(sym_table[i].symbol, "END") == 0){
            break;
        }
        printf("%s\t%04X\n",  sym_table[i].symbol, sym_table[i].addr);
    }
    
    printf("\n\n\n");
    
    for(int i = 0; i < literal_index; i++) {
        printf("%s\t%04X\n", literal_table[i].literal, literal_table[i].addr);
    }
    

    return 0;

}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명 혹은 경로
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char *file_name)
{
   /* add your code here */
    FILE *fp;
     
    // 파일명이 NULL인 경우 stdout으로 출력
    if(file_name == NULL || strlen(file_name) == 0)
    {
        fp = stdout;
    } else {
        fp = fopen(file_name, "w");
        if (fp == NULL){
            perror("파일열기실패");
            return;
        }
    }
     
    fprintf(fp, "Symbol\taddress\n");
    fprintf(fp, "-----------------------------\n");
    
    for(int i = 0; i < sym_index; i++) {
        if(strcmp(sym_table[i].symbol, "RDREC") == 0 || strcmp(sym_table[i].symbol, "WRREC") == 0 || strcmp(sym_table[i].symbol, "END") == 0)
            fprintf(fp, "\n");
        if(strcmp(sym_table[i].symbol, "END") == 0){
            break;
        }
        fprintf(fp, "%s\t%04X\n",  sym_table[i].symbol, sym_table[i].addr);
    }
     
    if(fp != stdout){
        fclose(fp);
    }
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char* file_name)
{
   /* add your code here */
    FILE* fp;

        // 파일명이 NULL이거나 비어 있으면 stdout으로 출력
        if (file_name == NULL || strlen(file_name) == 0) {
            fp = stdout;
        } else {
            fp = fopen(file_name, "w");
            if (fp == NULL) {
                perror("파일 열기 실패");
                return;
            }
        }

        fprintf(fp, "Literal\tAddress\n");
        fprintf(fp, "-------------------\n");

        for (int i = 0; i < literal_index; i++) {
            fprintf(fp, "%s\t%04X\n", literal_table[i].literal, literal_table[i].addr);
        }

        if (fp != stdout) {
            fclose(fp);
        }
}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다.
*         패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다.
*         다음과 같은 작업이 수행되어 진다.
*         1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다.
* 매계 : 없음
* 반환 : 정상종료 = 0, 에러발생 = < 0
* 주의 :
* -----------------------------------------------------------------------------------
*/
// nixbpe를 결정하는 함수
void set_nixbpe(token* tk, int token_idx) {
    int idx = search_opcode(tk->operator);
    if (idx < 0) return;

    inst* inst_info = inst_table[idx];
    tk->nixbpe = 0;

    if (inst_info->format == 1 || inst_info->format == 2) {
        return;
    }

    if (tk->operand[0] && tk->operand[0][0] == '@') {
        tk->nixbpe |= 0b100000;
    } else if (tk->operand[0] && tk->operand[0][0] == '#') {
        tk->nixbpe |= 0b010000;
    } else {
        tk->nixbpe |= 0b110000;
    }

    int is_format4 = (tk->operator[0] == '+');
    if(is_format4){
        tk->nixbpe |= 0b000001;
    }
    
    // 리스트의 마지막이 X임을 찾기 위함
    unsigned long len = strlen(tk->operand[0]);
    if (tk->operand[1] && tk->operand[0][len - 1] == 'X') {
        tk->nixbpe |= 0b001000;  // x=1
        if (is_format4) {
            tk->nixbpe |= 0b110001;
        } else {
            int target_addr = -1;
            if (tk->operand[0]) {
                char* operand = tk->operand[0];
                if (operand[0] == '#' || operand[0] == '@') operand++;
                for (int j = 0; j < sym_index; j++) {
                    if (strcmp(sym_table[j].symbol, operand) == 0) {
                        target_addr = sym_table[j].addr;
                        break;
                    }
                }
            }

            int PC = -1;
            if (token_idx + 1 < token_line) {
                for (int k = token_idx + 1; k < token_line; k++) {
                    if (token_table[k]->operator) {
                        PC = token_table[k]->loc;
                        break;
                    }
                }
            }

            if (target_addr != -1 && PC != -1) {
                int disp = target_addr - PC;
                if (disp >= -2048 && disp <= 2047) {
                    tk->nixbpe |= 0b000010;  // p=1
                } else {
                    int base = 0;
                    disp = target_addr - base;
                    if (disp >= 0 && disp <= 4095) {
                        tk->nixbpe |= 0b000100;  // b=1
                    }
                }
            }
        }
    } else if (!is_format4) {
        int target_addr = -1;
        if (tk->operand[0]) {
            char* operand = tk->operand[0];
            if (operand[0] == '#' || operand[0] == '@') operand++;
            for (int j = 0; j < sym_index; j++) {
                if (strcmp(sym_table[j].symbol, operand) == 0) {
                    target_addr = sym_table[j].addr;
                    break;
                }
            }
        }

        int PC = -1;
        if (token_idx + 1 < token_line) {
            for (int k = token_idx + 1; k < token_line; k++) {
                if (token_table[k]->operator) {
                    PC = token_table[k]->loc;
                    break;
                }
            }
        }

        if (target_addr != -1 && PC != -1) {
            int disp = target_addr - PC;
            if (disp >= -2048 && disp <= 2047) {
                tk->nixbpe |= 0b000010;  // p=1
            } else {
                int base = 0;
                disp = target_addr - base;
                if (disp >= 0 && disp <= 4095) {
                    tk->nixbpe |= 0b000100;  // b=1
                }
            }
        }
    }
}
// Register get 함수
int get_register_number(const char* reg) {
    if (strcmp(reg, "A") == 0) return 0;
    if (strcmp(reg, "X") == 0) return 1;
    if (strcmp(reg, "L") == 0) return 2;
    if (strcmp(reg, "B") == 0) return 3;
    if (strcmp(reg, "S") == 0) return 4;
    if (strcmp(reg, "T") == 0) return 5;
    if (strcmp(reg, "F") == 0) return 6;
    if (strcmp(reg, "PC") == 0) return 8;
    if (strcmp(reg, "SW") == 0) return 9;
    return 0;
}

// opcode 만드는 함수
char* generate_object_code(token* tk) {
    int idx = search_opcode(tk->operator);
    if (idx >= 0) {
        inst* inst_info = inst_table[idx];

        if (inst_info->format == 1) {
            char* obj_str = (char*)malloc(3);
            sprintf(obj_str, "%02X", inst_info->op);
            return obj_str;
        }

        if (inst_info->format == 2) {
            int r1 = 0, r2 = 0;
            if (tk->operand[0]) r1 = get_register_number(tk->operand[0]);
            if (tk->operand[1]) r2 = get_register_number(tk->operand[1]);

            char* obj_str = (char*)malloc(5);
            sprintf(obj_str, "%02X%1X%1X", inst_info->op, r1, r2);
            return obj_str;
        }

        unsigned int opcode = inst_info->op & 0xFC;
        unsigned int ni = (tk->nixbpe >> 4) & 0x3;
        unsigned int xbpe = tk->nixbpe & 0xF;

        unsigned int object_code = 0;
        int target_addr = 0;
        if (tk->operand[0]) {
            char* operand = tk->operand[0];
            if (operand[0] == '#' || operand[0] == '@') operand++;

            for (int i = 0; i < sym_index; i++) {
                if (strcmp(sym_table[i].symbol, operand) == 0) {
                    target_addr = sym_table[i].addr;
                    break;
                }
            }
        }

        if (tk->nixbpe & 0b000001) {
            object_code = ((opcode | ni) << 24) | ((xbpe << 20) | 0b00000);
        } else {
            if(strcmp(tk->operator, "RSUB") == 0){
                object_code = (opcode | ni) << 16;
                object_code |= ((xbpe << 12) | 0b000);
            }
            else{
                object_code = (opcode | ni) << 16;
                int disp = target_addr - tk->loc - 3;
                object_code |= ((xbpe << 12) | (disp & 0xFFF));
            }
        }

        char* obj_str = (char*)malloc(9);
        sprintf(obj_str, "%06X", object_code);
        return obj_str;
    }

    if (strcmp(tk->operator, "WORD") == 0 && tk->operand[0]) {
        char* obj_str = (char*)malloc(9);
        int value = atoi(tk->operand[0]);
        sprintf(obj_str, "%06X", value);
        return obj_str;
    }

    if (strcmp(tk->operator, "BYTE") == 0 && tk->operand[0]) {
        char* obj_str = (char*)malloc(9);
        if (tk->operand[0][0] == 'X') {
            strncpy(obj_str, tk->operand[0] + 2, strlen(tk->operand[0]) - 3);
            obj_str[strlen(tk->operand[0]) - 3] = '\0';
        } else if (tk->operand[0][0] == 'C') {
            int len = strlen(tk->operand[0]) - 3;
            char* lit = tk->operand[0] + 2;
            obj_str[0] = '\0';
            for (int i = 0; i < len; i++) {
                char tmp[3];
                sprintf(tmp, "%02X", lit[i]);
                strcat(obj_str, tmp);
            }
        }
        return obj_str;
    }

    return NULL;
}

int literal_written[10] = {0};

int generate_literal_object_int(const char* literal) {
    if (!literal) return -1;
    int result = 0;

    if (literal[1] == 'X') {
        sscanf(literal + 3, "%2x", &result);
    } else if (literal[1] == 'C') {
        const char* p = literal + 3;
        for (int i = 0; i < (int)(strlen(literal) - 4); i++) {
            result <<= 8;
            result |= (unsigned char)p[i];
        }
    }
    return result;
}

void append_literal_to_text_buffer(char* buffer, int* buffer_len, int* record_start, int addr, const char* literal) {
    if (!literal || !buffer || !buffer_len) return;

    char hex[10] = {0};
    if (literal[1] == 'X') {
        strncpy(hex, literal + 3, 2);
        hex[2] = '\0';
    } else if (literal[1] == 'C') {
        const char* p = literal + 3;
        hex[0] = '\0';
        for (int i = 0; i < (int)(strlen(literal) - 4); i++) {
            char tmp[3];
            sprintf(tmp, "%02X", (unsigned char)p[i]);
            strcat(hex, tmp);
        }
    }

    if (*record_start == -1) *record_start = addr;

    strcat(buffer, hex);
    *buffer_len += strlen(hex);

    for (int l = 0; l < literal_index; l++) {
        if (strcmp(literal_table[l].literal, literal) == 0) {
            literal_written[l] = 1;
            break;
        }
    }
}

void generate_text_record(FILE* fp, int start_idx, int end_idx) {
    int max_text_len = 60;
    char buffer[70] = {0};
    int buffer_len = 0;
    int record_start = -1;

    for (int i = start_idx; i <= end_idx; i++) {
        token* tk = token_table[i];
        if (!tk->operator) continue;
        
        if(strcmp(tk->operator, "CSECT") == 0) {
            extref_2_index = 0;
            for(int i = 0; i < MAX_OPERAND; i++){
                extref_2_table[i] = NULL;
            }
        }

        if (strcmp(tk->operator, "LTORG") == 0) {
            for (int l = 0; l < literal_index; l++) {
                if (!literal_written[l] && literal_table[l].addr >= 0 && literal_table[l].addr >= tk->loc) {
                    if (buffer_len > 0) {
                        fprintf(fp, "T%06X%02X%s\n", record_start, buffer_len / 2, buffer);
                        buffer[0] = '\0';
                        buffer_len = 0;
                        record_start = -1;
                    }
                    append_literal_to_text_buffer(buffer, &buffer_len, &record_start, literal_table[l].addr, literal_table[l].literal);
                }
            }
            continue;
        }

        int idx = search_opcode(tk->operator);
        if (idx < 0 && strcmp(tk->operator, "WORD") != 0 && strcmp(tk->operator, "BYTE") != 0) continue;

        char* obj = generate_object_code(tk);
        if (!obj) continue;

        if (record_start == -1) record_start = tk->loc;

        if (buffer_len + strlen(obj) > max_text_len) {
            fprintf(fp, "T%06X%02X%s\n", record_start, buffer_len / 2, buffer);
            buffer[0] = '\0';
            buffer_len = 0;
            record_start = tk->loc;
        }

        strcat(buffer, obj);
        buffer_len += strlen(obj);
        free(obj);
    }

    for (int l = 0; l < literal_index; l++) {
        if (!literal_written[l] && literal_table[l].addr != -1) {
            if (buffer_len + 2 > max_text_len) {
                fprintf(fp, "T%06X%02X%s\n", record_start, buffer_len / 2, buffer);
                buffer[0] = '\0';
                buffer_len = 0;
                record_start = -1;
            }
            if (record_start == -1) record_start = literal_table[l].addr;
            append_literal_to_text_buffer(buffer, &buffer_len, &record_start, literal_table[l].addr, literal_table[l].literal);
        }
    }

    if (buffer_len > 0) {
        fprintf(fp, "T%06X%02X%s\n", record_start, buffer_len / 2, buffer);
    }
}
// 피연산자 추출

void generate_modification_records(FILE* fp, int start_idx, int end_idx) {
    for (int i = start_idx; i <= end_idx; i++) {
        token* tk = token_table[i];
        if(strcmp(tk->operator, "EXTREF") == 0) {
           extref_2_index = sep_by_comma(tk->operand[0], extref_2_table, MAX_OPERAND);
        }
        
        if (!tk->operator || tk->operator[0] != '+' && strcmp(tk->operator, "WORD") != 0) continue;
        printf("%s", tk->operand[0]);
        for(int i = 0; i < extref_index; i++) {
            printf("%s", extref_2_table[i]);
        }
        if(tk->operand[0]){
            char* temp_array[MAX_OPERAND];
            int temp_each;
            temp_each = sep_by_comma(tk->operand[0], temp_array, MAX_OPERAND);
            for(int k = 0; k < strlen(tk->operand[0]); k++) {
                if(tk->operand[0][k] == '-'){
                    const char* pos = strpbrk(tk->operand[0], "+-*/");
                    char* operands[2];
                    num_operator = *pos;
                    long len_left = pos - tk->operand[0];
                    operands[0] = (char*)malloc(len_left+1);
                    strncpy(operands[0], tk->operand[0], len_left);
                    operands[0][len_left] = '\0';
                    operands[1] = strdup(pos + 1);
                    // printf("%s / %s \n", operands[0], operands[1]);
                    
                    strcpy(temp_array[0], operands[0]);
                    strcpy(temp_array[1], operands[1]);
                }
            }
            
            for(int j = 0; j < temp_each; j++){
                if(is_in_extref_2_list(temp_array[j]) == 0){
                    fprintf(fp, "M%06X05+%s\n", tk->loc + 1, temp_array[j]);
                }
            }
        }
    }
}

static int assem_pass2(void)
{
    // 기계어로 바꾸기 위해 토큰에 nixbpe를 부여한다.
    for (int i = 0; i < token_line; i++) {
        token* tk = token_table[i];
        if (!tk->operator) continue;
        set_nixbpe(tk, i);
    }
    return 0;
}

void make_objectcode_output(char *file_name) {
    FILE *fp = (file_name == NULL || strlen(file_name) == 0) ? stdout : fopen(file_name, "w");
    if (fp == NULL) {
        perror("파일 열기 실패");
        return;
    }

    int section_start = 0;

    for (int i = 0; i < token_line;) {
        token* tk = token_table[i];
        if (!tk->label || !tk->operator) {
            i++;
            continue;
        }

        char section_name[10];
        strcpy(section_name, tk->label);
        section_start = tk->loc;

        int section_end = section_start;
        int j = i;
        for (; j < token_line; j++) {
            if (token_table[j]->operator && strcmp(token_table[j]->operator, "CSECT") == 0 && j != i)
                break;
            section_end = token_table[j]->loc;
        }

        fprintf(fp, "H%-6s%06X%06X\n", section_name, section_start, section_end - section_start);

        int def_count = 0;
        for (int k = i; k < j; k++) {
            token* t = token_table[k];
            if (t->label && is_in_extdef_list(t->label) == 0)
                def_count++;
        }
        if (def_count > 0) {
            fprintf(fp, "D");
            for (int k = i; k < j; k++) {
                token* t = token_table[k];
                if (t->label && is_in_extdef_list(t->label) == 0)
                    fprintf(fp, "%-6s%06X", t->label, t->loc);
            }
            fprintf(fp, "\n");
        }

        fprintf(fp, "R");
        for (int k = i; k < j; k++) {
            token* t = token_table[k];
            if (strcmp(t->operator, "EXTREF") == 0) {
                char* ext_list[MAX_OPERAND];
                int list_length = sep_by_comma(t->operand[0], ext_list, MAX_OPERAND);
                for (int i = 0; i < list_length; i++) {
                    fprintf(fp, "%-6s", ext_list[i]);
                }
            }
        }
        fprintf(fp, "\n");

        generate_text_record(fp, i, j - 1);
        generate_modification_records(fp, i, j - 1);
        fprintf(fp, "E%06X\n\n\n", section_start);

        i = j;
    }

    if (fp != stdout) fclose(fp);
}



