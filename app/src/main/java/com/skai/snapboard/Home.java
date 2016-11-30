package com.skai.snapboard;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    // Variables for camera-on-shake
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 800;

    // Databases
    private QuadroDBHelper quadroDBHelper;
    private SubjectDBHelper subjectDBHelper;

    // Variables for ListView
    private SimpleCursorAdapter boardListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request Permissions
        String[] permissions =
                {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                 Manifest.permission.CAMERA,
                 Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(Home.this, permissions, 1);

        // Initialize Shake Sensors
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // Set Layout
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Floating Camera Button
/*        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newBoard();
            }
        });
        fab.setBackgroundColor(0x3F51B5);*/

        // Drawer Menu Layout
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize DB
        quadroDBHelper = new QuadroDBHelper(this);
        subjectDBHelper = new SubjectDBHelper(this);
        // if (quadroDBHelper != null) clearAll();

        //Generate ListView from SQLite Database
        if (quadroDBHelper.fetchAllBoards() != null) displayListView();

        // Try add new Board or edit Board
        Board newBoard = (Board)getIntent().getSerializableExtra("newBoard");
        Board editBoard = (Board)getIntent().getSerializableExtra("editBoard");
        Board deleteBoard = (Board)getIntent().getSerializableExtra("deleteBoard");
        if (newBoard != null) { quadroDBHelper.addBoard(newBoard); boardListAdapter.notifyDataSetChanged(); }
        if (editBoard != null) { quadroDBHelper.updateBoard(editBoard); boardListAdapter.notifyDataSetChanged(); }
        if (deleteBoard != null) { quadroDBHelper.deleteBoard(deleteBoard); boardListAdapter.notifyDataSetChanged(); }
        }

    // Initialize DB
    private void clearAll() {
        quadroDBHelper.getWritableDatabase().delete(QuadroDBHelper.DATABASE_NAME, null, null);
    }



    //----------------------------------Answer Permissions Request----------------------------
    //----------------------------------------------------------------------------------------
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(Home.this, "Permissions Denied\n App may not work properly", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    
    
    
    //---------------------------------List View Handler--------------------------------------
    //----------------------------------------------------------------------------------------
    private void displayListView() {


        Cursor cursor = quadroDBHelper.fetchAllBoards();

        // The desired columns to be bound
        String[] columns = new String[] {
                QuadroDBHelper.KEY_FILEPATH,
                QuadroDBHelper.KEY_SUBJECT,
                QuadroDBHelper.KEY_TAG,
                QuadroDBHelper.KEY_DATE
        };

        // the XML defined views which the data will be bound to
        int[] to = new int[] {
                R.id.board,
                R.id.subject,
                R.id.tag,
                R.id.date
        };

        // create the adapter using the cursor pointing to the desired data 
        //as well as the layout information
        boardListAdapter = new SimpleCursorAdapter(
                this, R.layout.board_item,
                cursor,
                columns,
                to,
                0);

        ListView listView = (ListView) findViewById(R.id.boardList);
        // Assign adapter to ListView
        listView.setAdapter(boardListAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                // Get the state's capital from this row in the database.
                String countryCode = cursor.getString(cursor.getColumnIndexOrThrow("code"));
                Toast.makeText(getApplicationContext(), countryCode, Toast.LENGTH_SHORT).show();

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> listView, View view, int position, long id) {
                Log.v("long clicked","pos: " + position);
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                Board editBoard = new Board(
                                        cursor.getInt(cursor.getColumnIndexOrThrow("_id")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("filePath")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("tag")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("date")),
                                        cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                                        cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
                editBoard(editBoard);
                return true;
            }
        }); 

        /*EditText myFilter = (EditText) findViewById(R.id.myFilter);
        myFilter.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                boardListAdapter.getFilter().filter(s.toString());
            }
        });

        boardListAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return quadroDBHelper.fetchBoardsBySubject(constraint.toString());
            }
        });*/

    }



    //----------------------------------Menu + Navigation-------------------------------------
    //----------------------------------------------------------------------------------------
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {newBoard();}
        else if (id == R.id.nav_subjects) {}
        else if (id == R.id.nav_add_subject) {newSubject();}

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    //----------------------------------------Main + Sensor-----------------------------------
    //----------------------------------------------------------------------------------------
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }
    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    newBoard();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



    //-----------------------------------------Quadros----------------------------------------
    //----------------------------------------------------------------------------------------
    public void newBoard() {
        Intent addBoardIntent = new Intent(Home.this, AddBoard.class);
        Home.this.startActivity(addBoardIntent);
    }

    public void editBoard(Board board) {
        Intent editBoardIntent = new Intent(Home.this, EditBoard.class);
        editBoardIntent.putExtra("editBoard", board);
        Home.this.startActivity(editBoardIntent);
    }




    //-----------------------------------------Matérias---------------------------------------
    //----------------------------------------------------------------------------------------
    private void newSubject() {
        // custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_add_subject);

        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button deleteButton = (Button) dialog.findViewById(R.id.deleteButton);
        // if button is clicked, close the custom dialog
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
