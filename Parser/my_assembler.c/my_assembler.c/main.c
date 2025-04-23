/*
 * 파일명 : my_assembler.c
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 *
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

 // 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20201551.h"

/* ------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일
 * 반환 : 성공 = 0, 실패 = < 0
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다.
 *           또한 중간파일을 생성하지 않는다.
 * ------------------------------------------------------------
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

    // make_symtab_output("output_symtab.txt");         //  추후 과제에 사용 예정
    // make_literaltab_output("output_littab.txt");     //  추후 과제에 사용 예정

    if (assem_pass2() < 0)
    {
        printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
        return -1;
    }

    // make_objectcode_output("output_objectcode.txt"); //  추후 과제에 사용 예정
}

// 토큰에 개행 문자를 모두 삭제하고 테이블에 넣도록 하기 위해 만듦
void trim(char* s) {
    s[strcspn(s, "\n\r\t ")] = '\0';  // 앞에서 처음 나오는 특수문자 위치를 '\0' 처리
}

/* ------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다.
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기
 *           위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *           구현하였다.
  * ------------------------------------------------------------
*/

int init_my_assembler(void)
{
    int result;

    if ((result = init_inst_file("inst_table.txt")) < 0)
        return -1;
    if ((result = init_input_file("input.txt")) < 0)
        return -1;
    return result;
}

/* ------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일(inst_table.txt)을 읽어
 *       기계어 목록 테이블(inst_table)을 생성하는 함수이다.
 *
 *
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 예시는 다음과 같다.
 *
 * =======================================================
 *           | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | \n |
 * ============================================================
 *
 * ------------------------------------------------------------
 */

int init_inst_file(char* inst_file)
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
    return 0;  // 정상 종료
}
    
/* ------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드 파일(input.txt)을 읽어 소스코드 테이블(input_data)를 생성하는 함수이다.
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 라인단위로 저장한다.
 *
 * ------------------------------------------------------------
 */
int init_input_file(char* input_file)
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

/* ------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다.
 *        패스 1로 부터 호출된다.
 * 매계 : 파싱을 원하는 문자열
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다.
 * ------------------------------------------------------------
 */
int token_parsing(char* str)
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

    // 첫 토큰이 opcode인 경우 → label 없음
    int opcode_idx = search_opcode(tok_list[0]);
    int is_label = (opcode_idx < 0); // 명령어가 아니면 label 취급
    
    // 집어넣기 전에 개행 먼저 삭제
    for(int i = 0; i < count; i++){
        trim(tok_list[i]);
    }
    
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


// opcode를 optable에서 찾기
int search_opcode(char* str)
{
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

/* ------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*           패스1에서는..
*           1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*           테이블을 생성한다.
*          2. 토큰 테이블은 token_parsing()을 호출하여 설정한다.
*          3. assem_pass2 과정에서 사용하기 위한 심볼테이블 및 리터럴 테이블을 생성한다.
*
*
*
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*         따라서 에러에 대한 검사 루틴을 추가해야 한다.
*
*        OPCODE 출력 프로그램에서는 심볼테이블, 리터럴테이블을 생성하지 않아도 된다.
*        그러나, 추후 프로젝트 1을 수행하기 위해서는 심볼테이블, 리터럴테이블이 필요하다.
*
* ------------------------------------------------------------
*/
static int assem_pass1(void)
{
    int i;
    token_line = 0;

    for (i = 0; i < line_num; i++) {
        if (input_data[i] == NULL || input_data[i][0] == '\0') continue;
        
        if (input_data[i][0] == '.'){
            printf("%s", input_data[i]);
        }
        
        if (token_parsing(input_data[i]) < 0) {
            fprintf(stderr, "Token parsing failed at line %d\n", i);
            return -1;
        }

        token* tk = token_table[token_line - 1];

        if(tk->operator == NULL) {
            continue;
        }
        char* label = tk->label ? tk->label : "";
        char* op    = tk->operator ? tk->operator : "";
        char* opd   = tk->operand[0] ? tk->operand[0] : "";
        
        // opcode 인덱스 찾기
        int idx = search_opcode(tk->operator);
        char opcode_buf[8];
        if (idx >= 0) {
            sprintf(opcode_buf, "%02X", inst_table[idx]->op);
        } else {
            strcpy(opcode_buf, " ");
        }

        printf("%-6s %-6s %-8s %s\n", label, op, opd, opcode_buf);
    }

    return 0;
}

/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 소스코드 명령어 앞에 OPCODE가 기록된 코드를 파일에 출력한다.
*        파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
*        OPCODE 출력 프로그램의 최종 output 파일을 생성하는 함수이다.
*        (추후 프로젝트 1에서는 불필요)
*
* ------------------------------------------------------------
*/
void make_opcode_output(char* file_name)
{
    /* add your code here */
}

/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명 혹은 경로
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* ------------------------------------------------------------
*/
void make_symtab_output(char* file_name)
{
    /* add your code here */
    
}


/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* ------------------------------------------------------------
*/
void make_literaltab_output(char* filename)
{
    
}


/* ------------------------------------------------------------
 * 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다.
 *           패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다.
 *           다음과 같은 작업이 수행되어 진다.
 *           1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다.
 * 매계 : 없음
 * 반환 : 정상종료 = 0, 에러발생 = < 0
 * 주의 :
 * ------------------------------------------------------------
 */


static int assem_pass2(void)
{
    
    return 0;

}

/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 object code이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        명세서의 주어진 출력 결과와 완전히 동일해야 한다.
*        예외적으로 각 라인 뒤쪽의 공백 문자 혹은 개행 문자의 차이는 허용한다.
*
* ------------------------------------------------------------
*/
void make_objectcode_output(char* file_name)
{
   
}







