package com.graduate.thesis.dto;

import lombok.Data;

/**
 * ER 图边: 源/目标节点 + 嵌入文字(基数) + 文字位置
 */
@Data
public class EREdge {
    private String id;
    private String source;
    private String target;
    /** 嵌入连线的文字(如 1/m/n), 空则不嵌入 */
    private String relationText;
    /** 文字在连线上的相对位置 0~1 */
    private float textPosition = 0.5f;
}
