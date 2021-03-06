package com.skai.snapboard;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

import static android.R.color.transparent;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    // Variables for camera-on-shake
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private int shakeThreshold = 800;
    private boolean isShakeOn = true;
    private static final int HIGH = 800;
    private static final int LOW = 1600;

    // Databases
    private BoardDBHelper boardDBHelper;
    private SubjectDBHelper subjectDBHelper;

    // Variable for Drawer
    private DrawerLayout drawer;
    private Menu menu;

    // Variable for Preferences
    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request Permissions
        String[] permissions =
                {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                 Manifest.permission.CAMERA,
                 Manifest.permission.ACCESS_FINE_LOCATION,
                 Manifest.permission.INTERNET};
        ActivityCompat.requestPermissions(Home.this, permissions, 1);

        isShakeOn = true;
        shakeThreshold = LOW;

        // Read Settings
        settings = this.getSharedPreferences("com.skai.snapboard", Context.MODE_PRIVATE);
        if (settings!=null) {
            isShakeOn = settings.getBoolean("com.skai.snapboard.is_shake_on", true);
            shakeThreshold = settings.getInt("com.skai.snapboard.threshold", LOW);
            System.out.println(isShakeOn+": "+shakeThreshold);
        }

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
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize DB
        boardDBHelper = new BoardDBHelper(this);
        subjectDBHelper = new SubjectDBHelper(this);

        // Refresh Drawer
        menu = navigationView.getMenu();
        if (subjectDBHelper != null) populateDrawer();

        //Generate ListView from SQLite Database
        if (boardDBHelper.fetchAllBoards() != null) displayListView();

        // Try add new Board or edit Board
        Board newBoard = (Board)getIntent().getSerializableExtra("newBoard");
        Board editBoard = (Board)getIntent().getSerializableExtra("editBoard");
        Board deleteBoard = (Board)getIntent().getSerializableExtra("deleteBoard");
        if (newBoard != null) { boardDBHelper.addBoard(newBoard); displayListView(); }
        if (editBoard != null) { boardDBHelper.updateBoard(editBoard); displayListView(); }
        if (deleteBoard != null) { boardDBHelper.deleteBoard(deleteBoard); displayListView(); }
        }

    // Initialize DB
    private void clearAll() {
        boardDBHelper.getWritableDatabase().delete(BoardDBHelper.DATABASE_NAME, null, null);
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


        Cursor cursor = boardDBHelper.fetchAllBoards();

        // The desired columns to be bound
        String[] columns = new String[] {
                BoardDBHelper.KEY_FILEPATH,
                BoardDBHelper.KEY_SUBJECT,
                BoardDBHelper.KEY_TAG,
                BoardDBHelper.KEY_DATE
        };

        // the XML defined views which the data will be bound to
        int[] to = new int[] {
                R.id.board,
                R.id.subject,
                R.id.tag,
                R.id.date
        };

        // Your database schema
        String[] mProjection = {
                BoardDBHelper.KEY_FILEPATH,
                BoardDBHelper.KEY_SUBJECT,
                BoardDBHelper.KEY_TAG,
                BoardDBHelper.KEY_DATE
        };

        // create the adapter using the cursor pointing to the desired data
        //as well as the layout information
        final BoardCustomAdapter boardListAdapter = new BoardCustomAdapter(this, cursor, 0);

        ListView listView = (ListView) findViewById(R.id.boardList);
        // Assign adapter to ListView
        listView.setAdapter(boardListAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file:"+cursor.getString(cursor.getColumnIndexOrThrow("filePath"))), "image/*");
                startActivity(intent);
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

        EditText myFilter = (EditText) findViewById(R.id.tagFilter);
        myFilter.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boardListAdapter.getFilter().filter(s.toString());
            }
        });

        boardListAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return boardDBHelper.fetchBoardsByTag(constraint.toString());
            }
        });

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
            // custom dialog
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.activity_settings);

            // Variables from dialog
            final Button noneButton = (Button) dialog.findViewById(R.id.noneButton);
            final Button lowButton = (Button) dialog.findViewById(R.id.lowButton);
            final Button highButton = (Button) dialog.findViewById(R.id.highButton);

            // Initialize
            if (isShakeOn) noneButton.setText(R.string.none_button_on); else noneButton.setText(R.string.none_button_off);
            if (shakeThreshold == LOW) lowButton.setText(R.string.low_button_off); else lowButton.setText(R.string.low_button_on);
            if (shakeThreshold == HIGH) highButton.setText(R.string.high_button_off); else highButton.setText(R.string.high_button_on);

            // Button Handler
            noneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isShakeOn) {
                        noneButton.setText(R.string.none_button_off);
                        isShakeOn = false;
                        settings.edit().putBoolean("com.skai.snapboard.is_shake_on", isShakeOn).apply();
                    }
                    else {
                        noneButton.setText(R.string.none_button_on);
                        isShakeOn = true;
                        settings.edit().putBoolean("com.skai.snapboard.is_shake_on", isShakeOn).apply();
                    }
                }
            });
            lowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lowButton.setText(R.string.low_button_on);
                    highButton.setText(R.string.high_button_off);
                    shakeThreshold = HIGH;
                    settings.edit().putInt("com.skai.snapboard.threshold", shakeThreshold).apply();
                }
            });
            highButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    highButton.setText(R.string.high_button_on);
                    lowButton.setText(R.string.low_button_off);
                    shakeThreshold = LOW;
                    settings.edit().putInt("com.skai.snapboard.threshold", shakeThreshold).apply();
                }
            });
            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {newBoard();}
        else if (id == R.id.nav_add_subject) {newSubject();}
        else if (id == R.id.nav_subjects) {
            EditText filter = (EditText)findViewById(R.id.tagFilter);
            filter.setText("");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }




    //----------------------------------Drawer Items------------------------------------------
    //----------------------------------------------------------------------------------------
    private void populateDrawer() {
        // Get Subjects
        List<Subject> allSubjects = subjectDBHelper.getAllSubjects();

        // Populate Subjects
        for (final Subject subject : allSubjects) {
            final MenuItem item = menu.add(0, subject.getId(), 0, subject.getSubject());
            item.setIcon(R.drawable.ic_menu_gallery);
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    drawer.closeDrawer(GravityCompat.START);
                    EditText filter = (EditText)findViewById(R.id.tagFilter);
                    filter.setText(subject.getSubject());
                    return true;
                }
            });
            /** set action view */
            ImageButton imageButton = new ImageButton(this);
            imageButton.setImageResource(R.mipmap.btn_close_small);
            imageButton.setBackgroundColor(getResources().getColor(transparent));
            item.setActionView(imageButton); // this is a Context.class object
            /** set listener  on action view */
            item.getActionView().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    deleteSubject(subject.getSubject());
                    menu.removeItem(subject.getId());
                    return false;
                }
            });
        }
    }

    private void addToDrawer(final Subject subject) {
        final MenuItem item = menu.add(0, subject.getId(), 0, subject.getSubject());
        item.setIcon(R.drawable.ic_menu_gallery);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                drawer.closeDrawer(GravityCompat.START);
                EditText filter = (EditText)findViewById(R.id.tagFilter);
                filter.setText(subject.getSubject());
                return true;
            }
        });
        /** set action view */
        ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(R.mipmap.btn_close_small);
        imageButton.setBackgroundColor(getResources().getColor(transparent));
        item.setActionView(imageButton); // this is a Context.class object
        /** set listener  on action view */
        item.getActionView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                deleteSubject(subject.getSubject());
                menu.removeItem(subject.getId());
                return false;
            }
        });
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

                if (speed > shakeThreshold && isShakeOn) {
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



    //-----------------------------------------Boards-----------------------------------------
    //----------------------------------------------------------------------------------------
    public void newBoard() {
        Intent addBoardIntent = new Intent(Home.this, AddBoard.class);
        String subjectSuggestion = getSubjectSuggestion();
        addBoardIntent.putExtra("subjectSuggestion", subjectSuggestion);
        Home.this.startActivity(addBoardIntent);
    }

    public void editBoard(Board board) {
        Intent editBoardIntent = new Intent(Home.this, EditBoard.class);
        editBoardIntent.putExtra("editBoard", board);
        Home.this.startActivity(editBoardIntent);
    }

    public String getSubjectSuggestion() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String[] days = getResources().getStringArray(R.array.day_list);
        String today = "";
        int hourNow = calendar.get(Calendar.HOUR_OF_DAY);
        int minuteNow = calendar.get(Calendar.MINUTE);
        String suggestion = null;

        switch (day) {
            case Calendar.MONDAY: {     today = days[0]; break; }
            case Calendar.TUESDAY: {    today = days[1]; break; }
            case Calendar.WEDNESDAY: {  today = days[2]; break; }
            case Calendar.THURSDAY: {   today = days[3]; break; }
            case Calendar.FRIDAY: {     today = days[4]; break; }
            case Calendar.SATURDAY: {   today = days[5]; break; }
            case Calendar.SUNDAY: {     today = days[6]; break; }
        }

        List<Subject> allSubjects = subjectDBHelper.getAllSubjects();
        for (Subject subject : allSubjects) {
            String[] hourMinStart = subject.getStart().split(",");
            int hourStart = Integer.parseInt(hourMinStart[0]);
            int minuteStart = Integer.parseInt(hourMinStart[1]);
            String[] hourMinEnd = subject.getEnd().split(",");
            int hourEnd = Integer.parseInt(hourMinEnd[0]);
            int minuteEnd = Integer.parseInt(hourMinEnd[1]);
            System.out.println("--------------> "+subject.getSubject()+" "+hourStart+":"+minuteStart+" "+hourNow+":"+minuteNow+" "+hourEnd+":"+minuteEnd);
            if (today.compareTo(subject.getDay())==0) {
                if (hourStart == hourEnd) {
                    if ((minuteStart<=minuteNow)&&(minuteNow<=minuteEnd)) {
                        System.out.println("true c");
                        suggestion=subject.getSubject();
                    }
                }
                else if (hourStart == hourNow) {
                    System.out.println("true a");
                    if (minuteStart<=minuteNow) {
                        System.out.println("true a1");
                        suggestion=subject.getSubject();
                    }
                }
                else if ((hourStart<hourNow)&&(hourNow<hourEnd)) {
                    System.out.println("true b");
                    suggestion=subject.getSubject();
                }
                else if (hourEnd==hourNow) {
                    System.out.println("true b");
                    if (minuteNow <= minuteEnd) {
                        System.out.println("true b1");
                        suggestion = subject.getSubject();
                    }
                }
                else {
                    System.out.println("no match");
                }
            }
        }
        return suggestion;
    }



    //-----------------------------------------Subjects---------------------------------------
    //----------------------------------------------------------------------------------------
    // Variables for making Subject
    private Subject newSubject;
    private String subjectName = "SUBJECT";
    private String day = "day";
    private String start = "";
    private String end = "";

    private void newSubject() {
        // custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_add_subject);

        // Variables from dialog
        final Button startButton = (Button) dialog.findViewById(R.id.startButton);
        final Button endButton = (Button) dialog.findViewById(R.id.endButton);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button deleteButton = (Button) dialog.findViewById(R.id.deleteButton);
        final TimePicker timePicker = (TimePicker)dialog.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        // Button Handler
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start = timePicker.getCurrentHour()+","+timePicker.getCurrentMinute();
                startButton.setText(timePicker.getCurrentHour()+":"+timePicker.getCurrentMinute());
            }
        });
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                end = timePicker.getCurrentHour()+","+timePicker.getCurrentMinute();
                endButton.setText(timePicker.getCurrentHour()+":"+timePicker.getCurrentMinute());
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText subjectText = (EditText)dialog.findViewById(R.id.nameEditText);
                Spinner mySpinner= (Spinner)dialog.findViewById(R.id.daySpinner);
                subjectName = subjectText.getText().toString();
                day = mySpinner.getSelectedItem().toString();
                if (start.length()>0 && end.length()>0) {
                    newSubject = new Subject(subjectName, day, start, end);
                    subjectDBHelper.addSubject(newSubject);
                    addToDrawer(newSubject);
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                    dialog.dismiss();
                }
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void deleteSubject(String subjectName) {
        subjectDBHelper.deleteSubject(subjectName);
    }
}
