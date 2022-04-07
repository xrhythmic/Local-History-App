package com.xrhythmic.localhistoryapp.ui.map

import android.location.Address
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xrhythmic.localhistoryapp.MainActivity
import java.util.ArrayList

class DataViewModel : ViewModel() {

    private val currentUser = MutableLiveData<MutableMap<String, Any>>()
    val user: LiveData<MutableMap<String, Any>> get() = currentUser

    fun setCurrentUser(userData: MutableMap<String, Any>) {
        currentUser.value = userData
    }

    private val currentAddress = MutableLiveData<Address>()
    val address: LiveData<Address> get() = currentAddress

    fun setAddress(addressData: Address) {
        currentAddress.value = addressData
    }
}