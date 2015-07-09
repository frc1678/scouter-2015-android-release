package org.citruscircuits.scout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    private String jsonSchedule;
    public static int scoutID = 6;
    List<Map<String, Object>> qualificationSchedule;
    private int matchIndex = 0;
    public static int channel = 5;

    public ListView storedListView;

    public int SUICIDE_NOTE = 42;

    File root;
    File storedMatchesDirectory;

    boolean allowOverride = false;

    public final String PREFERENCES_FILE = "org.citruscircuits.scout";
    public final String PREFERENCES_SCHEDULE_KEY = "schedule";
    public final String PREFERENCES_MATCH_KEY = "match";
    public final String PREFERENCES_SCOUTID_KEY = "scoutid";

    StoredMatchesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            .getString(PREFERENCES_SCHEDULE_KEY, null);
        super.onCreate(savedInstanceState);


        root = this.getFilesDir();
        storedMatchesDirectory = new File(root + "/match_data/");

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        scoutID = Integer.parseInt(preferences.getString(
                PREFERENCES_SCOUTID_KEY, scoutID + ""));

        setContentView(R.layout.activity_main);

        Log.e("test", "I AM RED " + isRed());

        EditText matchTextField = (EditText) findViewById(R.id.matchTextField);

        matchTextField.setText("Q1");

        matchTextField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (s.length() > 1) {
                    updateMatchAndTeamUI();
                }
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        matchIndex = getMatchNumberFromDisk();

        jsonSchedule = getScheduleFromDisk();
        if (jsonSchedule != null) {
            // Parse jsonSchedule and put into array
            Log.e("stupid logcat", "Loading schedule from saved JSON");
            qualificationSchedule = new ArrayList<Map<String, Object>>();
            try {
                JSONArray schedule = new JSONArray(jsonSchedule);
                for (int i = 0; i < schedule.length(); i++) {
                    JSONObject matchJsonObject = schedule.getJSONObject(i);
                    Map<String, Object> match = new HashMap<String, Object>();
                    match.put("matchNumber", matchJsonObject.getInt("matchNumber"));
                    match.put("redAlliance", matchJsonObject.getJSONArray("redAlliance"));
                    match.put("blueAlliance", matchJsonObject.getJSONArray("blueAlliance"));
                    qualificationSchedule.add(match);
                    Log.e("test", "The qualificationSchedule is " + qualificationSchedule.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("slc", "Hit JSONException: " + e.getMessage());
            }

            updateMatchAndTeamUI();

        } else {
            Toaster.makeErrorToast("No match schedule...", Toast.LENGTH_LONG);
            Log.e("test", "Is this what is happening?");
        }


        Log.e("a", "JSONS=" + jsonSchedule);
        saveScheduleToDisk(jsonSchedule);

        setMatchOverride(false);

        storedListView = (ListView) findViewById(R.id.storedMatchesListView);
        adapter = new StoredMatchesAdapter(this,
                android.R.layout.simple_list_item_1);
        adapter.loadDir(storedMatchesDirectory);
        storedListView.setAdapter(adapter);
        storedListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                view.setClickable(true);
                Log.e("looo~g", "the onItemClick worked");
                try {
                    File[] arrayOfMatches = storedMatchesDirectory.listFiles();
                    InputStream is = new FileInputStream(
                            arrayOfMatches[position]);

                    int size = is.available();

                    byte[] buffer = new byte[size];

                    is.read(buffer);

                    is.close();

                    final String jsonString = new String(buffer, "UTF-8");

                    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                            .getDefaultAdapter();
                    if (mBluetoothAdapter == null) {
                        // Device does not support Bluetooth
                        Toaster.makeErrorToast("Bluetooth not connected",
                                Toast.LENGTH_LONG);
                        return;
                    }

                    Toaster.makeToast("Starting Upload...", Toast.LENGTH_LONG);

                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                            .getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (final BluetoothDevice device : pairedDevices) {
                            Timer btTimer = new Timer();
                            btTimer.schedule(new TimerTask() {

                                @Override
                                public void run() {
                                    Log.e("log", "Starting upload");
                                    ConnectThread connectThread = new ConnectThread(
                                            device, mBluetoothAdapter,
                                            jsonString, null,
                                            MainActivity.this, channel, true);
                                    connectThread.start();
                                }
                            }, 2000);

                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    view.setClickable(false);
                }
            }
        });
        matchIndex = getMatchNumberFromDisk();

        updateMatchAndTeamUI();
        EditText matchText = (EditText) findViewById(R.id.matchTextField);
        matchText.setText("Q" + (matchIndex + 1));
    }

    public Integer getMatchNumberFromDisk() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        return preferences.getInt(PREFERENCES_MATCH_KEY, 0);
    }

    public void writeMatchNumberToDisk(Integer matchNum) {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putInt(PREFERENCES_MATCH_KEY, matchNum);
        editor.commit();
    }

    public void downloadScheduleClicked() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toaster.makeErrorToast("Bluetooth not connected", Toast.LENGTH_LONG);
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                ConnectThread connectThread = new ConnectThread(device,
                        mBluetoothAdapter, null, null, this, channel, false);
                connectThread.start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_set_scout_id) {

            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Set Scout ID");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            alert.setView(input);

            final MainActivity myThis = this;
            final EditText matchTextField = (EditText) findViewById(R.id.matchTextField);
            final EditText team1TextField = (EditText) findViewById(R.id.team1TextField);
            final EditText team2TextField = (EditText) findViewById(R.id.team2TextField);
            final EditText team3TextField = (EditText) findViewById(R.id.team3TextField);
            alert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            try {
                                scoutID = Integer.parseInt(input.getText()
                                        .toString());
                                if (scoutID < 1 || scoutID > 6) {
                                    throw new NumberFormatException();
                                }
                            } catch (NumberFormatException e) {
                                Toaster.makeErrorToast("Invalid scout ID",
                                        Toast.LENGTH_SHORT);
                            }

                            SharedPreferences preferences = getSharedPreferences(
                                    PREFERENCES_FILE, Context.MODE_PRIVATE);
                            Editor editor = preferences.edit();
                            editor.putString(PREFERENCES_SCOUTID_KEY,
                                    Integer.toString(scoutID));
                            editor.commit();

                            updateMatchAndTeamUI();

                            InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(input.getWindowToken(),
                                    0);
                            imm.hideSoftInputFromWindow(
                                    matchTextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team1TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team2TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team3TextField.getWindowToken(), 0);
                        }
                    });

            alert.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {

                            InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(input.getWindowToken(),
                                    0);
                            imm.hideSoftInputFromWindow(
                                    matchTextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team1TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team2TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team3TextField.getWindowToken(), 0);
                        }
                    });

            alert.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            matchTextField.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            team1TextField.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            team2TextField.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            team3TextField.getWindowToken(), 0);
                }
            });

            alert.show();

        } else if (id == R.id.action_set_bluetooth_channel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Set Bluetooth Channel");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            alert.setView(input);

            final MainActivity myThis = this;
            final EditText matchTextField = (EditText) findViewById(R.id.matchTextField);
            final EditText team1TextField = (EditText) findViewById(R.id.team1TextField);
            final EditText team2TextField = (EditText) findViewById(R.id.team2TextField);
            final EditText team3TextField = (EditText) findViewById(R.id.team3TextField);
            alert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            channel = Integer.parseInt(input.getText()
                                    .toString());

                            InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(input.getWindowToken(),
                                    0);
                            imm.hideSoftInputFromWindow(
                                    matchTextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team1TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team2TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team3TextField.getWindowToken(), 0);
                        }
                    });

            alert.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {

                            InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(input.getWindowToken(),
                                    0);
                            imm.hideSoftInputFromWindow(
                                    matchTextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team1TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team2TextField.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(
                                    team3TextField.getWindowToken(), 0);
                        }
                    });

            alert.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            matchTextField.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            team1TextField.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            team2TextField.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(
                            team3TextField.getWindowToken(), 0);
                }
            });

            alert.show();
        } else if (id == R.id.action_fetch_data) {
            downloadScheduleClicked();
        } else if (id == R.id.action_override) {
            toggleMatchOverride();
        }
        return true;
    }

    public void saveScheduleToDisk(String json) {
        Log.e("test", "The JSON file is: " + json);
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);

        Log.e("test", preferences.toString());
        Editor editor = preferences.edit();
        editor.putString(PREFERENCES_SCHEDULE_KEY, json);
        editor.commit();

        jsonSchedule = getScheduleFromDisk();
        Log.e("testoooooo", getScheduleFromDisk());
        Log.e("test", getScheduleFromDisk().length() + "");
        if (jsonSchedule != null) {
            // Parse jsonSchedule and put into array
            Log.e("logcat", "Loading schedule from saved JSON");
            qualificationSchedule = new ArrayList<Map<String, Object>>();
            try {
                Log.e("test", "Does the try here actually happen?");
                JSONArray schedule = new JSONArray(jsonSchedule);
                for (int i = 0; i < schedule.length(); i++) {
                    JSONObject matchJsonObject = schedule.getJSONObject(i);
                    Map<String, Object> match = new HashMap<String, Object>();
                    JSONArray redAlliance = matchJsonObject.getJSONArray("redAlliance");
                    match.put("redAlliance", redAlliance);

                    JSONArray blueAlliance = matchJsonObject.getJSONArray("blueAlliance");
                    match.put("blueAlliance", blueAlliance);

                    qualificationSchedule.add(match);
                    Log.e("tests", qualificationSchedule.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("test", "Wait, so does this happen?");
                Log.e("testerror", e.getMessage());
            }

            this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    updateMatchAndTeamUI();
                }
            });

        }
    }

    private String getScheduleFromDisk() {
        Log.e("test", getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).getString(PREFERENCES_SCHEDULE_KEY, "Colin is awesome!"));
        return getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                .getString(PREFERENCES_SCHEDULE_KEY, "Colin is awesome!");
    }

    public List<Integer> jsonArrayToList(JSONArray jsonArray) {
        List<Integer> returnList = new ArrayList<Integer>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                Integer teamNumber = new Integer(jsonObject.getInt("number"));
                returnList.add(teamNumber);
            } catch (JSONException jsone) {
                Log.e("test", jsone.getMessage());
            }
        }

        return returnList;
    }

    public void setMatchIndex(int index) {
        if (qualificationSchedule != null
                && index < qualificationSchedule.size()) {
            matchIndex = index;
            updateMatchAndTeamUI();
        } else {
            Toaster.makeErrorToast("No more matches to scout!",
                    Toast.LENGTH_LONG);
        }
    }

    public void incrementMatchIndex() {
        setMatchIndex(matchIndex + 1);
        Log.e("s", "matchIndex=" + matchIndex);
        writeMatchNumberToDisk(matchIndex);
    }

    public void toggleMatchOverride() {
        setMatchOverride(!allowOverride);

    }

    public void setMatchOverride(boolean override) {
        allowOverride = override;

        EditText matchTextField = (EditText) findViewById(R.id.matchTextField);
        EditText team1TextField = (EditText) findViewById(R.id.team1TextField);
        EditText team2TextField = (EditText) findViewById(R.id.team2TextField);
        EditText team3TextField = (EditText) findViewById(R.id.team3TextField);

        matchTextField.setEnabled(allowOverride);
        team1TextField.setEnabled(allowOverride);
        team2TextField.setEnabled(allowOverride);
        team3TextField.setEnabled(allowOverride);

        updateMatchAndTeamUI();
    }

    // Updates and fills the match and team number text fields with data
    public void updateMatchAndTeamUI() {

        // Set the correct text and color for the alliance label TextView
        EditText team1TextField = (EditText) findViewById(R.id.team1TextField);
        EditText team2TextField = (EditText) findViewById(R.id.team2TextField);
        EditText team3TextField = (EditText) findViewById(R.id.team3TextField);
        TextView allianceTextView = (TextView) findViewById(R.id.allianceColorTextView);
        EditText matchTextField = (EditText) findViewById(R.id.matchTextField);

        if (isRed()) {
            allianceTextView.setText("Red Alliance");
            allianceTextView.setTextColor(Color.RED);
        } else {
            allianceTextView.setText("Blue Alliance");
            allianceTextView.setTextColor(Color.BLUE);
        }

        if (scoutID == 1 || scoutID == 4) {
            if (allowOverride) {
                team2TextField.setTextColor(Color.BLACK);
                team3TextField.setTextColor(Color.BLACK);
            } else {
                team2TextField.setTextColor(Color.LTGRAY);
                team3TextField.setTextColor(Color.LTGRAY);
            }

            team1TextField.setTextColor(Color.GREEN);
        } else if (scoutID == 2 || scoutID == 5) {
            if (allowOverride) {
                team1TextField.setTextColor(Color.BLACK);
                team3TextField.setTextColor(Color.BLACK);
            } else {
                team1TextField.setTextColor(Color.LTGRAY);
                team3TextField.setTextColor(Color.LTGRAY);
            }

            team2TextField.setTextColor(Color.GREEN);
        } else if (scoutID == 3 || scoutID == 6) {
            if (allowOverride) {
                team1TextField.setTextColor(Color.BLACK);
                team2TextField.setTextColor(Color.BLACK);
            } else {
                team1TextField.setTextColor(Color.LTGRAY);
                team2TextField.setTextColor(Color.LTGRAY);
            }

            team3TextField.setTextColor(Color.GREEN);
        }

        if (qualificationSchedule == null) {
            return;
        }

        // Get the alliance Match
        Map<String, Object> currentMatch = null;
        if (qualificationSchedule.size() > matchIndex) {
            currentMatch = qualificationSchedule.get(matchIndex);

            Log.e("test", "The current match is " + currentMatch.toString());

            List<Integer> teamNumbers;
            if (isRed()) {

                teamNumbers = jsonArrayToList((JSONArray)currentMatch.get("redAlliance"));
            } else {
                teamNumbers = jsonArrayToList((JSONArray)currentMatch.get("blueAlliance"));
            }
            // Set the team number text
            Log.e("testing", teamNumbers.toString());
            team1TextField.setText(teamNumbers.get(0) + "");
            team2TextField.setText(teamNumbers.get(1) + "");
            team3TextField.setText(teamNumbers.get(2) + "");

            JSONArray sobj = null;
            try {
                sobj = new JSONArray(getScheduleFromDisk());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                JSONArray alliance = null;
                Log.e("test", "Is the tablet red? " + isRed() + "");
                if (!isRed()) {
                    alliance = sobj.getJSONObject(matchIndex).getJSONArray("blueAlliance");
                } else {
                    alliance = sobj.getJSONObject(matchIndex).getJSONArray("redAlliance");
                }

                team1TextField.setText(alliance.getInt(0) + "");
                team2TextField.setText(alliance.getInt(1) + "");
                team3TextField.setText(alliance.getInt(2) + "");

                matchTextField.setText("Q" + (matchIndex + 1));

                Log.e("test", alliance.getInt(0) + "");
                Log.e("test", alliance.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {

            }

        } else {
            Toaster.makeErrorToast("End of Match Schedule", Toast.LENGTH_LONG);
        }
    }

    // When the scout button is tapped, read the team and match number info from
    // the text fields, and hand that data off to the MatchActivity

    public void scoutTapped(String scoutName) {
        Intent scoutIntent = new Intent(this, AutoMatchActivity.class);

        EditText team1TextField = (EditText) findViewById(R.id.team1TextField);
        EditText team2TextField = (EditText) findViewById(R.id.team2TextField);
        EditText team3TextField = (EditText) findViewById(R.id.team3TextField);
        EditText matchTextField = (EditText) findViewById(R.id.matchTextField);

        int teamNumber = 0;
        if (scoutID == 1 || scoutID == 4) {
            teamNumber = Integer.parseInt(team1TextField.getText().toString());
        } else if (scoutID == 2 || scoutID == 5) {
            teamNumber = Integer.parseInt(team2TextField.getText().toString());
        } else if (scoutID == 3 || scoutID == 6) {
            teamNumber = Integer.parseInt(team3TextField.getText().toString());
        }

        scoutIntent.putExtra("teamNumber", teamNumber);
        scoutIntent
                .putExtra("matchNumber", matchTextField.getText().toString());
        scoutIntent.putExtra("channel", channel);
        scoutIntent.putExtra("allianceColor", isRed());
        scoutIntent.putExtra("scoutName", scoutName);

        startActivityForResult(scoutIntent, SUICIDE_NOTE);
    }

    public void enterInit(View view) {
        // Make scouts fess up and stop being stupid.

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Enter your Initials");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                String name = input.getEditableText().toString();
                if (name.length() >= 2) {
                    scoutTapped(input.getEditableText().toString());
                } else {
                    Toaster.makeToast("Initials have at least 2 letters...",
                            Toast.LENGTH_LONG);
                }
            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    }
                });

        alert.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {

                InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        });

        alert.show();

    }

    public static boolean isRed() {
        return scoutID <= 3 ? true : false;
    }

    int uploadIndex = -1;

    public void onBluetoothFinish(final boolean success) {

        File[] arrayOfMatches = storedMatchesDirectory.listFiles();
        if (success && uploadIndex >= 0 && uploadIndex < arrayOfMatches.length) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    adapter.loadDir(storedMatchesDirectory);
                }
            });
        } else {
            uploadIndex++;
        }

        arrayOfMatches = storedMatchesDirectory.listFiles();

        Looper.prepare();
        try {
            if (success)
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toaster.makeToast("Match Uploaded!", Toast.LENGTH_LONG);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toaster.makeErrorToast("Uploading Failed... :(", Toast.LENGTH_LONG);
                    }
                });
            }
            if (uploadIndex >= arrayOfMatches.length) {
                Toaster.makeErrorToast("No matches left to upload...", Toast.LENGTH_LONG);
                return;
            }

            InputStream is = new FileInputStream(arrayOfMatches[uploadIndex]);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            final String jsonString = new String(buffer, "UTF-8");

            final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                Toaster.makeErrorToast("Bluetooth not connected",
                        Toast.LENGTH_LONG);
                return;
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                    .getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (final BluetoothDevice device : pairedDevices) {
                    Timer btTimer = new Timer();
                    btTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            Log.e("log", "Starting upload");
//							ConnectThread connectThread = new ConnectThread(
//									device, mBluetoothAdapter, jsonString,
//									null, MainActivity.this, channel, true);
//							connectThread.start();
                        }
                    }, 2000);

                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void nextMatch()
    {
        Log.e("log", "match++");
        if (qualificationSchedule != null
                && matchIndex < qualificationSchedule.size()) {
            incrementMatchIndex();
        } else {
            Toaster.makeErrorToast("End of Match Schedule",
                    Toast.LENGTH_LONG);
        }
        EditText matchText = (EditText) findViewById(R.id.matchTextField);
        matchText.setText("Q" + (matchIndex + 1));
        adapter.loadDir(storedMatchesDirectory);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("test", "Got here");
        Log.e("tst", qualificationSchedule.size() + "");

        if (resultCode == SUICIDE_NOTE) {
            Log.e("log", "match++");
            if (qualificationSchedule != null
                    && matchIndex < qualificationSchedule.size()) {
                Log.e("a", "mn=" + matchIndex);
                nextMatch();
                Log.e("a", "mn=" + matchIndex);

                Log.e("LOOK AT THIS", "Going to next match?");
            } else {
                Toaster.makeErrorToast("End of Match Schedule",
                        Toast.LENGTH_LONG);
            }
            EditText matchText = (EditText) findViewById(R.id.matchTextField);
            matchText.setText("Q" + (matchIndex + 1));
            adapter.loadDir(storedMatchesDirectory);
        }
    }

}