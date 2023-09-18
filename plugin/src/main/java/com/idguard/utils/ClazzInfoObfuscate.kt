package com.idguard.utils

import com.idguard.modal.ClazzInfo
import com.idguard.modal.ConstructorInfo
import com.idguard.modal.OverrideStatusEnum

object ClazzInfoObfuscate {
    /**
     * raw an obfuscate name cache
     * not important for is raw name is class or method or field
     */
    private val rawObfuscateMap = mutableMapOf<String, String>()

    private fun getOrGenClassObfuscateName(rawName: String): String {
        return rawObfuscateMap.getOrPut(rawName) {
            RandomNameHelper.genClassName(Pair(4, 8))
        }
    }

    private fun getOrGenFieldParamsObfuscateName(rawName: String): String {
        return rawObfuscateMap.getOrPut(rawName) {
            RandomNameHelper.genNames(1, Pair(2, 8), false, true).first()
        }
    }

    private fun getOrGenMethodObfuscateName(rawName: String): String {
        return rawObfuscateMap.getOrPut(rawName) {
            RandomNameHelper.genNames(1, Pair(4, 12), false, true).first()
        }
    }

    fun fillClassNameObfuscateInfo(clazzInfo: ClazzInfo, inWhiteList: Boolean = false) {
        clazzInfo.obfuscateClazzName = if (inWhiteList) {
            clazzInfo.rawClazzName
        } else {
            getOrGenClassObfuscateName(clazzInfo.rawClazzName)
        }
    }

    fun fillClassContentObfuscateInfo(clazzInfo: ClazzInfo, inWhiteList: Boolean) {
        clazzInfo.constructors.onEach { con: ConstructorInfo ->
            con.params.onEach { paramInfo ->
                paramInfo.obfuscateName = if (inWhiteList) {
                    paramInfo.rawName
                } else {
                    getOrGenFieldParamsObfuscateName(paramInfo.rawName)
                }
            }
        }

        clazzInfo.fieldList.onEach {
            it.obfuscateName = if (inWhiteList) {
                it.rawName
            } else {
                getOrGenFieldParamsObfuscateName(it.rawName)
            }
        }

        clazzInfo.methodList.onEach {
            it.obfuscateName = if (inWhiteList) {
                it.rawName
            } else {
                when (it.needObfuscate) {
                    OverrideStatusEnum.NEED -> {
                        getOrGenMethodObfuscateName(it.rawName)
                    }

                    OverrideStatusEnum.UN_CONFIRM -> {
                        it.rawName
                    }

                    OverrideStatusEnum.NOT_NEED -> {
                        it.rawName
                    }

                    else -> {
                        it.rawName
                    }
                }
            }
            it.params.onEach { paramInfo ->
                paramInfo.obfuscateName = if (inWhiteList) {
                    paramInfo.rawName
                } else {
                    getOrGenFieldParamsObfuscateName(paramInfo.rawName)
                }
            }
        }
    }
}