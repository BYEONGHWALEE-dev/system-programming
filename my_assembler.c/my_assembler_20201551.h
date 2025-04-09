#include <stdio.h>

#define MAX_INST 256
#define MAX_LINES 5000

#define MAX_COLUMNS 4
#define MAX_OPERAND 3

#define MAX_OBJ_RECORD 100
#define MAX_RECORD_LEN 100

/*------------------------------------------------------------
 * instruction 목록을 저장하는 구조체이다.
 * instruction 목록 파일로부터 정보를 받아와서 생성한다.
 * instruction 목록 파일에는 라인별로 하나의 instruction을 저장한다.
 *------------------------------------------------------------*/
typedef struct _inst {
    char str[10];          // 명령어 문자열 (예: ADD, SUB)
    unsigned char op;      // opcode (16진수 값)
    int format;            // 명령어 포맷 (1, 2, 3/4)
    int ops;               // 오퍼랜드 수
} inst;

// instruction 테이블 및 인덱스
inst* inst_table[MAX_INST];
int inst_index;

/*------------------------------------------------------------
 * 어셈블리할 소스코드를 파일로부터 불러와 라인별로 관리하는 테이블
 *------------------------------------------------------------*/
char* input_data[MAX_LINES];
static int line_num;

int label_num;

/*------------------------------------------------------------
 * 소스코드를 토큰 단위로 분리하여 저장하는 구조체
 * label, operator, operand 들을 저장한다.
 *------------------------------------------------------------*/
typedef struct _token {
    char* label;
    char* operator;
    char* operand[MAX_OPERAND];
    char comment[100];
    // char nixbpe; // 추후 프로젝트에서 사용
} token;

token* token_table[MAX_LINES];
static int token_line;

/*------------------------------------------------------------
 * 심볼 테이블을 위한 구조체 및 배열
 *------------------------------------------------------------*/
typedef struct _symbol {
    char symbol[10];
    int addr;
} symbol;

symbol sym_table[MAX_LINES];

/*------------------------------------------------------------
 * 리터럴 테이블을 위한 구조체 및 배열
 *------------------------------------------------------------*/
typedef struct _literal {
    char* literal;
    int addr;
} literal;

literal literal_table[MAX_LINES]; // 주의: 구조체 타입이 `symbol`로 정의됨 (수정 필요 시 변경)

/*------------------------------------------------------------
 * 오브젝트 코드 전체 정보를 담는 구조체
 * Header, Text, Modification, End 레코드를 모두 포함한다.
 *------------------------------------------------------------*/
typedef struct _object_code {
    char header_record[MAX_RECORD_LEN];                    // H 레코드
    char text_records[MAX_OBJ_RECORD][MAX_RECORD_LEN];     // T 레코드
    int text_record_count;

    char mod_records[MAX_OBJ_RECORD][MAX_RECORD_LEN];      // M 레코드
    int mod_record_count;

    char end_record[MAX_RECORD_LEN];                       // E 레코드
} object_code;

object_code objcode; // 전역 변수로 선언

/*------------------------------------------------------------
 * 기타 전역 변수
 *------------------------------------------------------------*/
static int locctr;
static char* input_file;
static char* output_file;

/*------------------------------------------------------------
 * 함수 선언
 *------------------------------------------------------------*/
int init_my_assembler(void);
int init_inst_file(char* inst_file);
int init_input_file(char* input_file);
int token_parsing(char* str);
int search_opcode(char* str);

static int assem_pass1(void);
static int assem_pass2(void);

void make_opcode_output(char* file_name);
void make_symtab_output(char* file_name);
void make_literaltab_output(char* file_name);
void make_objectcode_output(char* file_name);
