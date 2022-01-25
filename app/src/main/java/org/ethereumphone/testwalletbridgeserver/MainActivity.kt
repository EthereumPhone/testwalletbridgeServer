package org.ethereumphone.testwalletbridgeserver

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.Toast
import org.walletconnect.Session
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSession
import java.util.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.impls.FileWCSessionStore
import java.io.File

class MainActivity : AppCompatActivity(), Session.Callback {
    lateinit var client : OkHttpClient
    lateinit var moshi : Moshi
    lateinit var storage : FileWCSessionStore
    lateinit var config: Session.Config
    lateinit var session: Session
    private var txRequest: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        client = OkHttpClient.Builder().build()
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        storage = FileWCSessionStore(File(this.filesDir, "session_store.json").apply { createNewFile() }, moshi)
        val button = findViewById(R.id.button) as Button
        button.setOnClickListener {
            createConnection()
        }

        val transact = findViewById(R.id.transact) as Button
        transact.setOnClickListener {
            makeTransaction()
        }
    }
    fun createConnection() {

        val key = ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
        config = Session.Config(UUID.randomUUID().toString(), "http://localhost:8887", key)
        session = WCSession(config.toFullyQualifiedConfig(),
            MoshiPayloadAdapter(moshi),
            storage,
            OkHttpTransport.Builder(client, moshi),
            Session.PeerMeta(name = "ethOS test")
        )
        session.addCallback(this)
        session.offer()
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(config.toWCUri())
        try {
            startActivity(i)
        } catch (e : ActivityNotFoundException) {
            Toast.makeText(this, "No wallet installed", Toast.LENGTH_LONG).show()
        }
    }

    fun makeTransaction() {
        val from = session.approvedAccounts()?.first()
        val txRequest = System.currentTimeMillis()
        session.performMethodCall(
            Session.MethodCall.SendTransaction(
                txRequest,
                from!!,
                "0x24EdA4f7d0c466cc60302b9b5e9275544E5ba552",
                null,
                null,
                null,
                "0x5AF3107A4000",
                ""
            ),
            ::handleResponse
        )
        this.txRequest = txRequest
    }
    private fun handleResponse(resp: Session.MethodCall.Response) {
        if (resp.id == txRequest) {
            txRequest = null
            println(resp.result.toString())
        }
    }

    override fun onMethodCall(call: Session.MethodCall) {
        //TODO
        println(call.toString())
    }

    override fun onStatus(status: Session.Status) {
        //TODO
        println(status.toString())
    }
}