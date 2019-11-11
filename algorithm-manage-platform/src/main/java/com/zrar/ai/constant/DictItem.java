package com.zrar.ai.constant;

import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class DictItem {

    /**
     * MODEL_TYPE
     */
    public static final String MODEL_TYPE_MLEAP = "mleap";
    public static final String MODEL_TYPE_TENSORFLOW = "tensorflow";
    public static final String MODEL_TYPE_COMPOSE = "compose";

    /**
     * MODEL_PARAM
     */
    public static final String CUT_METHOD_COMPOSE = "compose";
    public static final String CUT_METHOD_WORD_CUT = "word_cut";
    public static final String CUT_METHOD_WORD_CUT_VSWZYC = "word_cut_vswzyc";
    public static final String CUT_METHOD_PHRASE_LIST = "phrase_list";
    public static final String CUT_METHOD_CHAR_CUT = "char_cut";

    /**
     * MODEL_NAME
     */
    public static final String MODEL_NAME_DIRTY_WORD = "dirty_word";
    public static final String MODEL_NAME_SENTIMENT_ANALYSIS = "sentiment_analysis";
    public static final String MODEL_NAME_IS_TAX_ISSUE = "is_tax_issue";
    public static final String MODEL_NAME_AP_BILSTM = "ap_bilstm";
    public static final String MODEL_NAME_SHEBAO = "shebao";
    public static final String MODEL_NAME_FIRSTALL = "firstall";
    public static final String MODEL_NAME_SYNTHESIS = "synthesis";
    public static final String MODEL_NAME_CITY_MANAGEMENT = "city_management";
    public static final String MODEL_NAME_ZNZX = "znzx";
    public static final String MODEL_NAME_RERANKING = "reranking";
    public static final String MODEL_NAME_BERT_MATCH = "bert_match";

    /**
     * EXCEPTION
     */
    public static final String EXCEPTION_FILE_CAN_NOT_BE_EMPTY = "FILE_CAN_NOT_BE_EMPTY";
    public static final String EXCEPTION_FILE_SAVE_ERROR = "FILE_SAVE_ERROR";
    public static final String EXCEPTION_AUTH_FAILED = "AUTH_FAILED";
    public static final String EXCEPTION_SCP_FAILED = "SCP_FAILED";
    public static final String EXCEPTION_FILE_IS_WRONG = "FILE_IS_WRONG";
    public static final String EXCEPTION_INVOKE_FAILED = "INVOKE_FAILED";
    public static final String EXCEPTION_MODEL_ONLINE_FAILED = "MODEL_ONLINE_FAILED";
    public static final String EXCEPTION_JSON_ERROR = "JSON_ERROR";
    public static final String EXCEPTION_PREDICT_ERROR = "PREDICT_ERROR";
    public static final String EXCEPTION_UNKNOWN_CUT_METHOD_ERROR = "UNKNOWN_CUT_METHOD_ERROR";
    public static final String EXCEPTION_UNKNOWN_NAME_ERROR = "UNKNOWN_NAME_ERROR";
    public static final String EXCEPTION_GET_MODEL_NAMES_ERROR = "GET_MODEL_NAMES_ERROR";
    public static final String EXCEPTION_CAN_NOT_FIND_MODEL_ERROR = "CAN_NOT_FIND_MODEL_ERROR";
    public static final String EXCEPTION_UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public static final String EXCEPTION_DEL_MODEL_ERROR = "DEL_MODEL_ERROR";
    public static final String EXCEPTION_ADD_MODEL_ERROR = "ADD_MODEL_ERROR";
    public static final String EXCEPTION_GET_PARAMS_ERROR = "GET_PARAMS_ERROR";
    public static final String EXCEPTION_UNKNOWN_MODEL_TYPE = "UNKNOWN_MODEL_TYPE";
    public static final String EXCEPTION_CMD_ERROR = "CMD_ERROR";
    public static final String EXCEPTION_FORM_ERROR = "FORM_ERROR";
    public static final String EXCEPTION_LOAD_STOP_WORD_ERROR = "LOAD_STOP_WORD_ERROR";
    public static final String EXCEPTION_RESTART_DOCKER_ERROR = "RESTART_DOCKER_ERROR";
    public static final String EXCEPTION_CREATE_DOCKER_ERROR = "CREATE_DOCKER_ERROR";
    public static final String EXCEPTION_START_DOCKER_ERROR = "START_DOCKER_ERROR";
    public static final String EXCEPTION_DELETE_DOCKER_ERROR = "DELETE_DOCKER_ERROR";
    public static final String EXCEPTION_STOP_DOCKER_ERROR = "STOP_DOCKER_ERROR";
    public static final String EXCEPTION_RECREATE_DOCKER_ERROR = "RECREATE_DOCKER_ERROR";
    public static final String EXCEPTION_MODEL_FILE_NOT_EXIST_ERROR = "MODEL_FILE_NOT_EXIST_ERROR";
    public static final String EXCEPTION_MODEL_EXIST_CANNOT_ADD_ERROR = "MODEL_EXIST_CANNOT_ADD_ERROR";
    public static final String EXCEPTION_CAN_NOT_FIND_DICT_DETAIL_ERROR = "CAN_NOT_FIND_DICT_DETAIL_ERROR";
}
