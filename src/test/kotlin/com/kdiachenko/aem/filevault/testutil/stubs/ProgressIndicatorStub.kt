package com.kdiachenko.aem.filevault.stubs

import com.intellij.openapi.progress.util.ProgressIndicatorBase

class ProgressIndicatorStub : ProgressIndicatorBase() {
    val textsChanges = arrayListOf<String>()
    val fractionChanges = arrayListOf<Double>()

    override fun setText(text: String?) {
        //super.setText(text)
        textsChanges.add(text ?: "")
    }

    override fun setFraction(fraction: Double) {
        //super.setFraction(fraction)
        fractionChanges.add(fraction)
    }

}
