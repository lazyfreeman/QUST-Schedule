package com.qust.schedule.model

import com.google.gson.annotations.SerializedName

/**
 * 映射正方教务系统 /jwglxt/kbcx/xskbcx_cxXsKb.html 返回的原始 JSON 结构
 */
data class RawZhengFangResponse(
    @SerializedName("kbList")
    val kbList: List<RawKbItem>? = null,
    @SerializedName("xnm")
    val xnm: String? = null,
    @SerializedName("xqm")
    val xqm: String? = null
)

data class RawKbItem(
    @SerializedName("kcmc")
    val kcmc: String? = null, // 课程名称

    @SerializedName("cdmc")
    val cdmc: String? = null, // 场地/教室名称

    @SerializedName("xm")
    val xm: String? = null, // 授课教师姓名

    @SerializedName("zcd")
    val zcd: String? = null, // 周次描述，如 "1-16周", "1-8周(单)", "2-16周(双)"

    @SerializedName("xqj")
    val xqj: String? = null, // 星期几 (1~7)

    @SerializedName("jcor")
    val jcor: String? = null, // 节次范围，如 "1-2"

    @SerializedName("kch_id")
    val kchId: String? = null, // 课程号

    @SerializedName("jxbmc")
    val jxbmc: String? = null // 教学班名称
)
