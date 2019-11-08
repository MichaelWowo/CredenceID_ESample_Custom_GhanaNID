package com.credenceid.sample.epassport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.BiometricsManager
import com.credenceid.biometrics.DeviceFamily

class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        this.initBiometrics()
    }

    private fun initBiometrics() {

        /*  Create new biometrics object. */
        App.BioManager = BiometricsManager(this)

        /* Initialize object, meaning tell CredenceService to bind to this application. */
        App.BioManager.initializeBiometrics { resultCode: Biometrics.ResultCode, _: String, _: String ->

            when (resultCode) {
                OK -> {
                    Toast.makeText(this, getString(R.string.bio_init_done), LENGTH_SHORT).show()

                    App.DevFamily = App.BioManager.deviceFamily
                    App.DevType = App.BioManager.deviceType

                    val intent = when (App.DevFamily) {
                        DeviceFamily.CredenceTAB ->
                            Intent(this, com.credenceid.sample.epassport.ctab.MRZActivity::class.java)
                        DeviceFamily.CredenceTwo ->
                            Intent(this, com.credenceid.sample.epassport.ctwo.MRZActivity::class.java)
                        else -> return@initializeBiometrics
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    this.finish()

                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Toast.makeText(this, getString(R.string.bio_ini_fail), LENGTH_LONG).show()
            }
        }
    }
}
