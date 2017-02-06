package com.example.andrea.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class DbHelper: SQLiteOpenHelper {
    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "MyMoney.db"
    }
    constructor(ctx: Context) : super(ctx, DATABASE_NAME, null, DATABASE_VERSION)

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("""
            DROP TABLE IF EXISTS transfers
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
            amount, INT
        )
        """)
    }
}

class TransfersCursorAdapter: CursorAdapter {
    constructor(context: Context, cursor: Cursor): super(context, cursor, 0)

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val desc = view?.findViewById(R.id.description) as TextView

        desc.text =
                cursor?.getString(
                cursor.getColumnIndexOrThrow("description"))
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

        list_view.adapter = adapter

        submit.setOnClickListener {
            if (description.text.isBlank()) {
                description.error = "La descrizione non puo essere vuota"
            } else if(amount.text.isBlank()) {
                amount.error = "La spesa non puo essere vuota"
            } else {
                // Clear errors
                description.error = null
                amount.error = null

                // Write to the db
                val values = ContentValues()
                values.put("description", description.text.toString())
                values.put("amount", amount.text.toString().toInt())

                val row_id = writeDb.insert("transfers", null, values)
                println("Inserted to $row_id")

                // Clear input
                description.text = null
                amount.text = null

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
