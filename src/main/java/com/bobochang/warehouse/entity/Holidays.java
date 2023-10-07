package com.bobochang.warehouse.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * tb_holidays
 * @author 
 */
@Data
public class Holidays implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 日期
     */
    private Date date;

    private static final long serialVersionUID = 1L;
}