package com.credenceid.sample.epassport.ctwo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.credenceid.biometrics.Biometrics.*
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.icao.ICAODocumentData
import com.credenceid.icao.ICAOReadIntermediateCode
import com.credenceid.sample.epassport.App
import com.credenceid.sample.epassport.R
import com.credenceid.sample.epassport.ctab.MRZActivity
import kotlinx.android.synthetic.main.act_mrz_ctwo.*

/**
 * Used for Android Logcat.
 */
private val TAG = MRZActivity::class.java.simpleName
/**
 * Keeps track of card reader sensor state.
 */
private var isCardReaderOpen = false
private var isDocumentPresent = false

class MRZActivity : Activity() {

    private var onCardStatusListener = OnCardStatusListener { _, _, currState ->
        if (currState in 2..6) {
            readICAOBtn.isEnabled = true
            isDocumentPresent = true
        } else {
            readICAOBtn.isEnabled = false
            isDocumentPresent = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_mrz_ctwo)
        this.configureLayoutComponents()
    }

    override fun onDestroy() {

        super.onDestroy()

        /* Make sure to close all peripherals on application exit. */
        App.BioManager!!.ePassportCloseCommand()
        App.BioManager!!.closeMRZ()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        openCardBtn.setOnClickListener {
            /* Based on current state of MRZ reader take appropriate action. */
            if (!isCardReaderOpen)
                openCardReader()
            else App.BioManager!!.cardCloseCommand()
        }

        readICAOBtn.isEnabled = false
        readICAOBtn.setOnClickListener {
            icaoDG2ImageView.setImageBitmap(null)
            this.readICAODocument(dobEditText.text.toString(),
                    docNumberEditText.text.toString(),
                    doeEditText.text.toString())
        }
    }

    private fun openCardReader() {

        icaoDG2ImageView.setImageBitmap(null)
        statusTextView.text = getString(R.string.cardreader_opening)

        App.BioManager!!.registerCardStatusListener(onCardStatusListener)

        App.BioManager!!.cardOpenCommand(object : CardReaderStatusListener {
            override fun onCardReaderOpen(resultCode: ResultCode?) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openCardBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isCardReaderOpen = true

                        statusTextView.text = getString(R.string.card_opened)
                        openCardBtn.text = getString(R.string.close_card)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = getString(R.string.card_open_fail)
                    }
                }
            }

            override fun onCardReaderClosed(resultCode: ResultCode,
                                            closeReasonCode: CloseReasonCode?) {
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openCardBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isCardReaderOpen = false

                        statusTextView.text = getString(R.string.card_closed)
                        openCardBtn.text = getString(R.string.open_cardreader)
                        readICAOBtn.isEnabled = false

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> statusTextView.text = getString(R.string.card_close_fail)
                }
            }
        })
    }

    /**
     * Calls Credence APIs to read an ICAO document.
     *
     * @param dateOfBirth Date of birth on ICAO document (YYMMDD format).
     * @param documentNumber Document number of ICAO document.
     * @param dateOfExpiry Date of expiry on ICAO document (YYMMDD format).
     */
    @SuppressLint("SetTextI18n")
    private fun readICAODocument(dateOfBirth: String?,
                                 documentNumber: String?,
                                 dateOfExpiry: String?) {

        /* If any one of three parameters is bad then do not proceed with document reading. */
        if (null == dateOfBirth || dateOfBirth.isEmpty()) {
            Log.w(TAG, "DateOfBirth parameter INVALID, will not read ICAO document.")
            return
        }
        if (null == documentNumber || documentNumber.isEmpty()) {
            Log.w(TAG, "DocumentNumber parameter INVALID, will not read ICAO document.")
            return
        }
        if (null == dateOfExpiry || dateOfExpiry.isEmpty()) {
            Log.w(TAG, "DateOfExpiry parameter INVALID, will not read ICAO document.")
            return
        }

        Log.d(TAG, "Reading ICAO document: $dateOfBirth, $documentNumber, $dateOfExpiry")

        /* Disable button so user does not initialize another readICAO document API call. */
        readICAOBtn.isEnabled = false
        statusTextView.text = getString(R.string.reading)

        App.BioManager!!.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry)
        { rc: ResultCode, stage: ICAOReadIntermediateCode, hint: String?, data: ICAODocumentData ->

            Log.d(TAG, "STAGE: " + stage.name + ", Status: " + rc.name + "Hint: $hint")
            Log.d(TAG, "ICAODocumentData: $data")

            statusTextView.text = "Finished reading stage: " + stage.name
            if (ICAOReadIntermediateCode.BAC == stage) {
                if (FAIL == rc) {
                    statusTextView.text = getString(R.string.bac_failed)
                    readICAOBtn.isEnabled = (isCardReaderOpen && isDocumentPresent)
                }

            } else if (ICAOReadIntermediateCode.DG1 == stage) {
                if (OK == rc)
                    icaoTextView.text = data.DG1.toString()

            } else if (ICAOReadIntermediateCode.DG2 == stage) {
                if (OK == rc) {
                    icaoTextView.text = data.DG2.toString()
                    icaoDG2ImageView.setImageBitmap(data.DG2.faceImage)
                }

            } else if (ICAOReadIntermediateCode.DG3 == stage) {
                if (OK == rc)
                    icaoTextView.text = data.DG3.toString()

            } else if (ICAOReadIntermediateCode.DG7 == stage) {
                if (OK == rc)
                    icaoTextView.text = data.DG7.toString()

            } else if (ICAOReadIntermediateCode.DG11 == stage) {
                if (OK == rc)
                    icaoTextView.text = data.DG1.toString()

            } else if (ICAOReadIntermediateCode.DG12 == stage) {
                if (OK == rc)
                    icaoTextView.text = data.DG12.toString()

                statusTextView.text = getString(R.string.icao_done)
                readICAOBtn.isEnabled = (isCardReaderOpen && isDocumentPresent)
            }
        }
    }
}