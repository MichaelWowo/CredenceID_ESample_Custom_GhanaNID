package com.credenceid.sample.epassport

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.credenceid.biometrics.Biometrics.*
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.icao.GIdData
import com.credenceid.icao.ICAODocumentData
import com.credenceid.icao.ICAOReadIntermediateCode
import kotlinx.android.synthetic.main.act_mrz_ctwo.*

/**
 * Used for Android Logcat.
 */
private val TAG = MRZActivity::class.java.simpleName
/**
 * Keeps track of card reader sensor state.
 */
private var isCardReaderOpen = false
private var isPassportReaderOpen = false
private var isDocumentPresent = false
private var CERTIFICATES_LOCAL_EID_PATH = "/sdcard/credenceid/GhanaNID/eid/"
private var CERTIFICATES_LOCAL_ICAO_PATH = "/sdcard/credenceid/GhanaNID/icao/"

class MRZActivity : Activity() {

    private var onCardStatusListener = OnCardStatusListener { _, _, currState ->
        if (currState in 2..6) {
            readGhanaIdBtn.isEnabled = true
            isDocumentPresent = true
        } else {
            setReadButtons (false)
            isDocumentPresent = false
        }
    }

    private var onEPassportStatusListener = OnEPassportStatusListener { _, currState ->
        Log.d(TAG, "Epasport status : " + currState)
        if (currState in 2..6) {
            readIcaoBtn.isEnabled = true
            isDocumentPresent = true
        } else {
            setReadButtons (false)
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
            if (!isCardReaderOpen)
                openCardReader()
            else App.BioManager!!.cardCloseCommand()
        }

        openEpassBtn.setOnClickListener {
            if (!isPassportReaderOpen)
                openMrzReader()
            else App.BioManager!!.ePassportCloseCommand()
        }

        setReadButtons (false)
        readGhanaIdBtn.setOnClickListener {
            icaoDG2ImageView.setImageBitmap(null)
            icaoTextView1.setText("")
            icaoTextView2.setText("")
            this.readGhanaIdDocument(docCanEditText.text.toString())
        }

        readIcaoBtn.setOnClickListener {
            icaoDG2ImageView.setImageBitmap(null)
            icaoTextView1.setText("")
            icaoTextView2.setText("")
            val mrzString = "I<GHAL898902C<3<<<<<<<<<<<<<<<" +
                    "8006226M2001012GHA<<<<<<<<<<<8" +
                    "HEMINGWAY<<ERNEST<<<<<<<<<<<<<"
            this.readICAODocument(mrzString)
        }

        generateCertificateRequestBtn.setOnClickListener {
            App.BioManager!!.generateTerminalIsCertificate(generateTerminalIsCertificateListener { resultCode, s ->
                when (resultCode) {
                    OK -> {
                        statusTextView.text = "Certificate generation OK"
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "Certificate generation failed"
                    }
                }
            }
            )
        }
    }

    private fun openCardReader() {

        icaoDG2ImageView.setImageBitmap(null)
        icaoTextView1.setText("")
        icaoTextView2.setText("")
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
                        setReadButtons (false)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> statusTextView.text = getString(R.string.card_close_fail)
                }
            }
        })
    }

    private fun openMrzReader() {

        icaoDG2ImageView.setImageBitmap(null)
        icaoTextView1.setText("")
        icaoTextView2.setText("")
        statusTextView.text = getString(R.string.mrz_opening)

        App.BioManager!!.openMRZ(object : MRZStatusListener {
            override fun onMRZOpen(resultCode: ResultCode?) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openCardBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */

                        statusTextView.text = getString(R.string.mrz_opened)
                        openEPassportReader();
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = getString(R.string.mrz_open_failed)
                    }
                }
            }

            override fun onMRZClose(resultCode: ResultCode,
                                                 closeReasonCode: CloseReasonCode?) {
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openCardBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */

                        statusTextView.text = getString(R.string.mrz_closed)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> statusTextView.text = getString(R.string.mrz_failed_close)
                }
            }
        })
    }

    private fun openEPassportReader() {

        icaoDG2ImageView.setImageBitmap(null)
        icaoTextView1.setText("")
        icaoTextView2.setText("")
        statusTextView.text = getString(R.string.epassport_opening)

        App.BioManager!!.registerEPassportStatusListener(onEPassportStatusListener)

        App.BioManager!!.ePassportOpenCommand(object : EPassportReaderStatusListener {
            override fun onEPassportReaderOpen(resultCode: ResultCode?) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openCardBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isPassportReaderOpen = true

                        statusTextView.text = getString(R.string.epassport_opened)
                        openEpassBtn.text = getString(R.string.close_epassport)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = getString(R.string.epassport_open_failed)
                    }
                }
            }

            override fun onEPassportReaderClosed(resultCode: ResultCode,
                                            closeReasonCode: CloseReasonCode?) {
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openCardBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isPassportReaderOpen = false

                        statusTextView.text = getString(R.string.epassport_closed)
                        openEpassBtn.text = getString(R.string.open_epassport)
                        setReadButtons (false)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> statusTextView.text = getString(R.string.epassport_open_failed)
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun  readGhanaIdDocument(can: String?) {

        /* If any one of three parameters is bad then do not proceed with document reading. */
        if (null == can || can.isEmpty()) {
            Log.w(TAG, "DateOfBirth parameter INVALID, will not read ICAO document.")
            return
        }

        Log.d(TAG, "Reading ID document: $can")

        /* Disable button so user does not initialize another readICAO document API call. */
        setReadButtons (false)
        statusTextView.text = getString(R.string.reading)

        App.BioManager!!.readSmartCard(can,CERTIFICATES_LOCAL_EID_PATH, "GhanaNID")
        { rc: ResultCode, stage: ICAOReadIntermediateCode, hint: String?, data: GIdData ->

            Log.d(TAG, "STAGE: " + stage.name + ", Status: " + rc.name + "Hint: $hint")
            Log.d("CID", "GhanaIdCardData: $data")

            when (rc) {
                OK -> {

                    statusTextView.text = "Finished reading stage: " + stage.name
                    if (ICAOReadIntermediateCode.BAC == stage) {
                        if (FAIL == rc) {
                            statusTextView.text = getString(R.string.bac_failed)
                            setReadButtons(isDocumentPresent)
                        }

                    } else if (ICAOReadIntermediateCode.DG1 == stage) {
                        if (OK == rc) {
                            icaoTextView1.text = "DG1 - " + data.DG1.toString()
                        }

                    } else if (ICAOReadIntermediateCode.DG2 == stage) {
                        if (OK == rc) {
                            icaoTextView1.text = icaoTextView1.text.toString() + "\nDG2 - " + data.DG2.toString()
                            icaoDG2ImageView.setImageBitmap(data.DG2.faceImage)
                        }

                    } else if (ICAOReadIntermediateCode.DG3 == stage) {
                        if (OK == rc)
                            icaoTextView1.text = icaoTextView1.text.toString() + "\nDG3 - " + data.DG3.toString()

                    } else if (ICAOReadIntermediateCode.DG4 == stage) {
                        if (OK == rc)
                            icaoTextView1.text = icaoTextView1.text.toString() + "\nDG4 - " + data.DG4.toString()

                    } else if (ICAOReadIntermediateCode.DG5 == stage) {
                        if (OK == rc)
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG5 - " + data.DG5.toString()

                    }  else if (ICAOReadIntermediateCode.DG6 == stage) {
                        if (OK == rc)
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG6 - " + data.DG6.toString()

                    } else if (ICAOReadIntermediateCode.DG7 == stage) {
                        if (OK == rc)
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG7 - " + data.DG7.toString()

                    }  else if (ICAOReadIntermediateCode.DG8 == stage) {
                        if (OK == rc)
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG8 - " + data.DG8.toString()

                    }  else if (ICAOReadIntermediateCode.DG9 == stage) {
                        if (OK == rc)
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG9 - " + data.DG9.toString()

                    }  else if (ICAOReadIntermediateCode.DG10 == stage) {
                        if (OK == rc) {
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG10 - " + data.DG10.toString()
                            Log.d(TAG, "MINUTIAE LENTGH = " + data.DG10.getFingers()[0].minutiae.size);
                            var FP1 = App.BioManager!!.convertCCFToFMDSync(data.DG10.getFingers()[1].minutiae, 350 , 450, 500, 500, 2000)
                            App.BioManager!!.convertCCFToFMD(data.DG10.getFingers()[0].minutiae, 350 , 450, 500, 500)
                            { rc: ResultCode,  temp ->
                                Log.d(TAG, "RESULT = " + rc);
                                Log.d(TAG, "RESULT LENTGH = " + temp.size);
                                App.BioManager!!.compareFMD(temp, FP1.FMD, FMDFormat.ISO_19794_2_2005)
                                { rc: ResultCode,  score ->
                                    Log.d(TAG, "RESULT COMPARE = " + rc)
                                    Log.d(TAG, "COMPARE SCORE = " + score)
                                }
                            }
                            App.BioManager!!.convertCCFToFMD(data.DG10.getFingers()[0].minutiae, 350 , 450, 500, 500)
                            { rc: ResultCode,  temp ->
                                Log.d(TAG, "RESULT = " + rc);
                                Log.d(TAG, "RESULT LENTGH = " + temp.size);
                                App.BioManager!!.compareFMD(temp, data.DG10.getFingers()[0].minutiae, FMDFormat.ISO_19794_2_2005)
                                { rc: ResultCode,  score ->
                                    Log.d(TAG, "RESULT COMPARE = " + rc)
                                    Log.d(TAG, "COMPARE FMD againt CCF SCORE = " + score)
                                }
                            }

                        }

                    } else if (ICAOReadIntermediateCode.DG11 == stage) {
                        if (OK == rc)
                            icaoTextView2.text = icaoTextView2.text.toString() + "\nDG11 - " + data.DG11.toString()

                        statusTextView.text = getString(R.string.icao_done)
                        setReadButtons (isDocumentPresent)
                    }
                }
                /* This code is never returned for this API. */
                INTERMEDIATE -> {
                }
                FAIL -> {
                    statusTextView.text = "Reading failed \n" + hint
                    statusTextView.text = getString(R.string.icao_done)
                    setReadButtons(isDocumentPresent)
                }
            }
        }
    }

    /**
     * Calls Credence APIs to read an ICAO document.
     *
     * @param dateOfBirth Date of birth on ICAO document (YYMMDD format).
     * @param documentNumber Document number of ICAO document.
     * @param dateOfExpiry Date of expiry on ICAO document (YYMMDD format).
     */
    @SuppressLint("SetTextI18n")
    private fun readICAODocument(mrz: String?) {


        Log.d(TAG, "Reading ICAO document: $mrz")

        /* Disable button so user does not initialize another readICAO document API call. */
        setReadButtons(false)
        statusTextView.text = getString(R.string.reading)

        App.BioManager!!.readSmartCard(mrz,
                CERTIFICATES_LOCAL_ICAO_PATH, "GhanaICAO"
        )
        //App.BioManager!!.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry)
        { rc: ResultCode, stage: ICAOReadIntermediateCode, hint: String?, data: ICAODocumentData ->

            Log.d(TAG, "STAGE: " + stage.name + ", Status: " + rc.name + "Hint: $hint")
            Log.d(TAG, "ICAODocumentData: $data")

            statusTextView.text = "Finished reading stage: " + stage.name
            if (ICAOReadIntermediateCode.BAC == stage) {
                if (FAIL == rc) {
                    statusTextView.text = getString(R.string.bac_failed)
                    setReadButtons(isDocumentPresent)
                }

            } else if (ICAOReadIntermediateCode.DG1 == stage) {
                if (OK == rc) {
                    Log.d("CPC", "DG1 DATA = "+ data.DG1.toString());
                    icaoTextView2.text = data.DG1.toString()
                }

            } else if (ICAOReadIntermediateCode.DG2 == stage) {
                if (OK == rc) {
                    icaoTextView1.text = data.DG2.toString()
                    icaoDG2ImageView.setImageBitmap(data.DG2.faceImage)
                }

            } else if (ICAOReadIntermediateCode.DG3 == stage) {
                if (OK == rc) {
                    icaoTextView1.text = "Number of fingers" + data.DG3.fingers.size
                    icaoTextView1.text = icaoTextView1.text.toString() + "\n" +
                            data.DG3.toString()
                    if(data.DG3.fingers.size > 0)

                        icaoTextView1.text = icaoTextView1.text.toString() + "\n" +
                                data.DG3.fingers[0].bytes.size
                        icaoDG2ImageView.setImageBitmap(data.DG3.fingers[0].bitmap)
                }

                statusTextView.text = getString(R.string.icao_done)
                setReadButtons(isDocumentPresent)
            }
        }
    }

    private fun setReadButtons(enable: Boolean){
        readIcaoBtn.isEnabled = enable
        readGhanaIdBtn.isEnabled = enable
    }
}

