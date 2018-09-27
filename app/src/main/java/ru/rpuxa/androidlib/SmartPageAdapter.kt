package ru.rpuxa.androidlib

import android.app.Activity
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter


class SmartPageAdapter private constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val fragments = ArrayList<Fragment>()

    override fun getItem(position: Int) = fragments[position]

    override fun getCount() = fragments.size

    companion object {

        fun FragmentActivity.smartPageAdapter(block: Builder.() -> Unit): SmartPageAdapter {
            val adapter = SmartPageAdapter(supportFragmentManager)
            adapter.Builder().block()
            return adapter
        }
    }

    inner class Builder {
        operator fun Fragment.unaryPlus() {
            fragments.add(this)
        }

        fun layout(@LayoutRes id: Int) {
            fragments.add(SmartFragment.create<LayoutSmartFragment>(id))
        }
    }
}