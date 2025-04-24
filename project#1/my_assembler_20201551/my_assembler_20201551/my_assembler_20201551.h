/*
 * my_assembler 함수를 위한 변수 선언 및 매크로를 담고 있는 헤더 파일이다.
 *
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3

#define MAX_COLUMNS 4

#define MAX_OBJ_RECORD 100
#define MAX_RECORD_LEN 100


 /*
  * instruction 목록 파일로 부터 정보를 받아와서 생성하는 구조체 변수이다.
  * 라인 별로 하나의 instruction을 저장한다.
  */
typedef struct _inst
{
   char str[10];
   unsigned char op;
   int format;
   int ops;
} inst;

inst* inst_table[MAX_INST];
int inst_index;

/*
 * 어셈블리 할 소스코드를 입력받는 테이블이다. 라인 단위로 관리할 수 있다.
 */
char* input_data[MAX_LINES];
static int line_num;

/*
 * 어셈블리 할 소스코드를 토큰단위로 관리하기 위한 구조체 변수이다.
 * operator는 renaming을 허용한다.
 */
typedef struct _token
{
   char* label;
   char* operator;
   char* operand[MAX_OPERAND];
   char comment[100];
   char nixbpe;
    int loc ;
} token;

token* token_table[MAX_LINES];
static int token_line;

/*
 * 심볼을 관리하는 구조체이다.
 * 심볼 테이블은 심볼 이름, 심볼의 위치로 구성된다.
 * 추후 과제에 사용 예정
 */
typedef struct _symbol
{
   char symbol[10];
   int addr;
} symbol;
int sym_index;

/*
* 리터럴을 관리하는 구조체이다.
* 리터럴 테이블은 리터럴의 이름, 리터럴의 위치로 구성된다.
* 추후 과제에 사용 예정
*/
typedef struct _literal {
   char* literal;
   int addr;
} literal;

symbol sym_table[MAX_LINES];
literal literal_table[MAX_LINES];
int literal_index;

/**
 * 오브젝트 코드 전체에 대한 정보를 담는 구조체이다.
 * Header Record, Define Recode,
 * Modification Record 등에 대한 정보를 모두 포함하고 있어야 한다. 이
 * 구조체 변수 하나만으로 object code를 충분히 작성할 수 있도록 구조체를 직접
 * 정의해야 한다.
 */

typedef struct _object_code {
   /* add fields */
} object_code;

// 개인이 따로 선언한 함수
// EXTDEF, EXTREF 저장
char* extdef_table[MAX_OPERAND];
int extdef_index;
char* extref_table[MAX_OPERAND];
int extref_index;

// 연산자 저장(-,+,*,/)
char num_operator;

static int locctr;

// pass2에서 사용할 ext_table
char* extref_2_table[MAX_OPERAND];
int extref_2_index;


// utility
void trim(char* str);
int is_in_direct_list(const char* str);
int is_in_extref_list(const char* str);
int is_in_extdef_list(const char* str);
int is_in_extdef_2_list(const char* str);
int extract_operands(char* str, char* str_2[]);
int is_numeric(char* str);
void update_ext_tables_on_directives(token* token);
void set_nixbpe(token* token, int integer);
int get_register_number(const char* str);
char* generate_object_code(token* token);
int generate_literal_object_int(const char* str);
void append_literal_to_text_buffer(char* buffer, int* buffer_len, int* record_start, int addr, const char* literal);
void generate_text_record(FILE* fp, int start_idx, int end_idx);
void generate_modification_records(FILE* fp, int start_idx, int end_idx);

// Given
//--------------

static char* input_file;
static char* output_file;
int init_my_assembler(void);
int init_inst_file(char* inst_file);
int init_input_file(char* input_file);
int token_parsing(char* str);
int search_opcode(char* str);
static int assem_pass1(void);

void make_literaltab_output(char* file_name);
void make_symtab_output(char* file_name);
static int assem_pass2(void);
void make_objectcode_output(char* file_name);
