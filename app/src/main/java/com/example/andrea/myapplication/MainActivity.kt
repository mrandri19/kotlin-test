package com.example.andrea.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DataSetObserver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CursorAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class DbHelper: SQLiteOpenHelper {
    companion object {
        const val DATABASE_VERSION = 5
        const val DATABASE_NAME = "MyMoney.db"
    }
    constructor(ctx: Context) : super(ctx, DATABASE_NAME, null, DATABASE_VERSION)

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("""
            DROP TABLE IF EXISTS transfers
        """)
        db?.execSQL("""
        CREATE TABLE transfers
        (
            _id INTEGER PRIMARY KEY,
            description TEXT,
            amount INT
        )
        """)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        super.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
        CREATE TABLE transfers
        (
            _id INTEGER PRIMARY KEY,
            description TEXT,
            amount INT
        )
        """)
    }
}

class TransfersCursorAdapter: CursorAdapter {
    constructor(context: Context, cursor: Cursor): super(context, cursor, 0)

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_activated_2, parent, false)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val desc = view?.findViewById(android.R.id.text1) as TextView
        val amount = view?.findViewById(android.R.id.text2) as TextView

        amount.text =
                cursor?.getInt(
                cursor.getColumnIndexOrThrow("amount"))
                        .toString()
        amount.setTextColor(Color.parseColor("#9E9E9E"))

        desc.text =
                cursor?.getString(
                cursor.getColumnIndexOrThrow("description"))

        val id = cursor?.getString(
                cursor.getColumnIndexOrThrow("_id")
        )

        view?.setOnLongClickListener { v ->
            println("Deleting $id")
            val dbHelper = DbHelper(context!!)
            val writeDb = dbHelper.writableDatabase
            val readDb = dbHelper.readableDatabase

            writeDb.delete("transfers", "_id = $id", null)

            this.notifyDataSetChanged()


            val cursor = readDb.query("transfers", arrayOf("_id", "description", "amount"), null, null, null, null, null)
            this.changeCursor(cursor)
            true
        }
    }
}

class MainObserver constructor (private val e: TextView, private val ctx: Context): DataSetObserver() {
    override fun onChanged() {
        val dbHelper = DbHelper(ctx)
        val readDb = dbHelper.readableDatabase
        val cursor = readDb.rawQuery("""
            SELECT SUM(amount) FROM transfers
        """, null)
        if(cursor.moveToNext()) {
            e.text = cursor.getInt(0).toString()
        }
        cursor.close()
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init db helper
        val dbHelper = DbHelper(this)
        val readDb = dbHelper.readableDatabase
        val writeDb = dbHelper.writableDatabase
        val cursor = readDb.query("transfers", arrayOf("_id", "description", "amount"), null, null, null, null, null)

        val adapter = TransfersCursorAdapter(this, cursor)
        adapter.registerDataSetObserver(MainObserver(total_money, this))

        input_amount.setOnEditorActionListener { textView, actionId, keyEvent ->
            if(actionId == EditorInfo.IME_ACTION_DONE) {
                submit.performClick()
                true
            }
            false
        }

        list_view.adapter = adapter

        submit.setOnClickListener {
            if (input_description.text.isBlank()) {
                input_description.error = "La descrizione non puo essere vuota"
            } else if(input_amount.text.isBlank()) {
                input_amount.error = "La spesa non puo essere vuota"
            } else {
                // Clear errors
                input_description.error = null
                input_amount.error = null

                // Write to the db
                val values = ContentValues()
                values.put("description", input_description.text.toString())
                val amount = input_amount.text.toString().toFloat()
                val amount_decimal = -(amount * 100).toInt()

                values.put("amount", amount_decimal)

                val row_id = writeDb.insert("transfers", null, values)
                println("Inserted to $row_id")

                // Clear input
                input_description.text = null
                input_amount.text = null

                // Update the adapter
                adapter.notifyDataSetChanged()

                // Close the old cursor, the query has already been done
                cursor.close()

                // Create a new one
                val cursor = readDb.query("transfers", arrayOf("_id", "description", "amount"), null, null, null, null, null)
                adapter.changeCursor(cursor)
            }
        }
    }
}
