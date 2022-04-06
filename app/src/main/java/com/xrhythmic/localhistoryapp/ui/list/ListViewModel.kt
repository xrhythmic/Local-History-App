package com.xrhythmic.localhistoryapp.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ListViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is List View Fragment"
    }
    val text: LiveData<String> = _text
}