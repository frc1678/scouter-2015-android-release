package org.citruscircuits.scout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TeleMatchActivity extends Activity {

    public boolean shouldUpload = false;
    public List<Map<String, Object>> coopActions = new ArrayList<Map<String, Object>>();

    private ToggleButton t1;
    private ToggleButton t2;
    private ToggleButton t3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_teleop);

        Intent oldIntent = getIntent();
        Log.e("test", oldIntent.toString());

        TextView topTextView = (TextView)findViewById(R.id.teamLabel);
        topTextView.setText(oldIntent.getIntExtra("teamNumber", 0) + "");
        if (MainActivity.isRed()) {
            topTextView.setTextColor(Color.RED);
        } else {
            topTextView.setTextColor(Color.BLUE);
        }

        t1 = (ToggleButton) findViewById(R.id.toggle1);
        t2 = (ToggleButton) findViewById(R.id.toggle2);
        t3 = (ToggleButton) findViewById(R.id.toggle3);

        clicked1(findViewById(R.id.toggle1));
        clicked2(findViewById(R.id.toggle2));
        clicked3(findViewById(R.id.toggle3));

        Button successButton = (Button)findViewById(R.id.successButton);
        Button failButton = (Button)findViewById(R.id.failButton);

        successButton.setOnClickListener(coopSubmitListener);
        failButton.setOnClickListener(coopSubmitListener);

        ToggleButton toggle1 = (ToggleButton) findViewById(R.id.toggle1);
        ToggleButton toggle2 = (ToggleButton) findViewById(R.id.toggle2);
        ToggleButton toggle3 = (ToggleButton) findViewById(R.id.toggle3);

        toggle1.setOnClickListener(toggleListener);
        toggle2.setOnClickListener(toggleListener);
        toggle3.setOnClickListener(toggleListener);

        disableSubmitButtons();
    }

    private View.OnClickListener toggleListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ToggleButton toggle1 = (ToggleButton) findViewById(R.id.toggle1);
            ToggleButton toggle2 = (ToggleButton) findViewById(R.id.toggle2);
            ToggleButton toggle3 = (ToggleButton) findViewById(R.id.toggle3);

            toggle1.setChecked(false);
            toggle2.setChecked(false);
            toggle3.setChecked(false);

            Button successButton = (Button)findViewById(R.id.successButton);
            successButton.setEnabled(true);
            Button failButton = (Button)findViewById(R.id.failButton);
            failButton.setEnabled(true);

            ((ToggleButton)view).setChecked(true);
        }
    };

    private View.OnClickListener coopSubmitListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Button clickedCoopSubmitButton = (Button)view;

            Map<String, Object> coopAction = new HashMap<String, Object>();

            ToggleButton toggle1 = (ToggleButton) findViewById(R.id.toggle1);
            ToggleButton toggle2 = (ToggleButton) findViewById(R.id.toggle2);
            ToggleButton toggle3 = (ToggleButton) findViewById(R.id.toggle3);

            int coopTotes = 1;
            if(toggle1.isChecked()) {
                coopTotes = 1;
                toggle1.setChecked(false);
            } else if(toggle2.isChecked()) {
                coopTotes = 2;
                toggle2.setChecked(false);
            } else if(toggle3.isChecked()) {
                coopTotes = 3;
                toggle3.setChecked(false);
            }

            ToggleButton topButton = (ToggleButton)findViewById(R.id.topToggle);

            boolean top = topButton.isChecked();
            topButton.setChecked(false);

            boolean didSucceed;
            if(clickedCoopSubmitButton.getText() == "Success") {
                didSucceed = true;
            } else {
                didSucceed = false;
            }

            coopAction.put("numTotes", coopTotes);
            coopAction.put("onTop", top);
            coopAction.put("didSucceed", didSucceed);

            disableSubmitButtons();

            coopActions.add(coopAction);
        }
    };

    public void disableSubmitButtons() {

        Button succeedButton = (Button)findViewById(R.id.successButton);
        succeedButton.setEnabled(false);

        Button failButton = (Button)findViewById(R.id.failButton);
        failButton.setEnabled(false);
    }

    public void clicked1(View view) {
        boolean on = ((ToggleButton) view).isChecked();
            if(on) {
                t2.setSelected(false);
                t3.setSelected(false);
            }
        }

        public void clicked2(View view) {
            boolean on = ((ToggleButton) view).isChecked();
            if(on) {
                t1.setSelected(false);
                t3.setSelected(false);
            }
        }

        public void clicked3(View view) {
            boolean on = ((ToggleButton) view).isChecked();
            if(on) {
                t1.setSelected(false);
                t2.setSelected(false);
            }
        }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure you want to stop scouting?");
        alert.setMessage("You will have to restart the match!");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                TeleMatchActivity.this.setResult(666);
                TeleMatchActivity.this.finish();
            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        alert.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tele_match, menu);
        return true;
    }

    public void plusMinusButtonClicked(View view) {
        LinearLayout parent = (LinearLayout) view.getParent();
        TextView v = (TextView)parent.getChildAt(2);
        v.setText(Integer.toString(Integer.parseInt((String)v.getText())+(((String)(view.getTag())).contains("Plus") ? 1 : -1)));
    }


	private void writeToFile(String text, String fileName) {
		File root = this.getFilesDir();

		File dir = new File(root + "/match_data/");
		dir.mkdirs();

		File file = new File(dir, fileName);

		try {
			FileOutputStream f = new FileOutputStream(file);
			PrintWriter pw = new PrintWriter(f);
			pw.println(text);
			pw.flush();
			pw.close();
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		if (item.getItemId() == R.id.action_done) {
			Log.e("logcat sucks", "Done pressed");

			setResult(Activity.RESULT_OK);

			Intent intent = getIntent();

            TextView numTotesStackedText = (TextView)findViewById(R.id.toteStackedAmt);
            TextView numReconLevelsText = (TextView)findViewById(R.id.reconLevelAmt);
            TextView numNoodlesText = (TextView)findViewById(R.id.noodleAmt);
            TextView numReconStackedText = (TextView)findViewById(R.id.reconStackAmt);
            TextView numVerticalReconsPickedUpText = (TextView)findViewById(R.id.reconPickUpVerticalAmt);
            TextView numHorizontalReconsPickedUpText = (TextView)findViewById(R.id.reconPickUpHorizontalAmt);
            TextView numSixStacksCappedText = (TextView)findViewById(R.id.numSixStacksCappedAmt);
            TextView numTotesFromGroundText = (TextView)findViewById(R.id.toteGroundAmt);
            TextView numTotesFromHPText = (TextView)findViewById(R.id.toteHPAmt);
            TextView numLitterDroppedText = (TextView)findViewById(R.id.litterAmt);
            TextView numDamagedText = (TextView)findViewById(R.id.damageStackAmt);
            TextView maxOnFieldText = (TextView)findViewById(R.id.toteHeightAmt);
            TextView maxReconHeightText = (TextView)findViewById(R.id.reconHeightAmt);
            TextView numReconsFromStepText = (TextView)findViewById(R.id.numReconsFromStepAmt);

            int numTotesStacked = Integer.parseInt(numTotesStackedText.getText().toString());
            int numReconLevels = Integer.parseInt(numReconLevelsText.getText().toString());
            int numNoodles = Integer.parseInt(numNoodlesText.getText().toString());
            int numReconsStacked = Integer.parseInt(numReconStackedText.getText().toString());
            int numVerticalReconsPickedUp = Integer.parseInt(numVerticalReconsPickedUpText.getText().toString());
            int numHorizontalReconsPickedUp = Integer.parseInt(numHorizontalReconsPickedUpText.getText().toString());
            int numTotesFromGround = Integer.parseInt(numTotesFromGroundText.getText().toString());
            int numTotesFromHP = Integer.parseInt(numTotesFromHPText.getText().toString());
            int numLitterDropped = Integer.parseInt(numLitterDroppedText.getText().toString());
            int numDamaged = Integer.parseInt(numDamagedText.getText().toString());
            int maxOnField = Integer.parseInt(maxOnFieldText.getText().toString());
            int maxReconHeight = Integer.parseInt(maxReconHeightText.getText().toString());
            int numSixStacksCapped = Integer.parseInt(numSixStacksCappedText.getText().toString());
            int numReconsFromStep = Integer.parseInt(numReconsFromStepText.getText().toString());

            Map<String, Object> teleData = new HashMap<String, Object>();
            teleData.put("numTotesStacked", numTotesStacked);
            teleData.put("numReconLevels", numReconLevels);
            teleData.put("numNoodlesContributed", numNoodles);
            teleData.put("numReconsStacked", numReconsStacked);
            teleData.put("numVerticalReconsPickedUp", numVerticalReconsPickedUp);
            teleData.put("numHorizontalReconsPickedUp", numHorizontalReconsPickedUp);
            teleData.put("numTotesPickedUpFromGround", numTotesFromGround);
            teleData.put("numTotesFromHP", numTotesFromHP);
            teleData.put("numLitterDropped", numLitterDropped);
            teleData.put("numStacksDamaged", numDamaged);
            teleData.put("maxFieldToteHeight", maxOnField);
            teleData.put("maxReconHeight", maxReconHeight);
            teleData.put("numSixStacksCapped", numSixStacksCapped);
            teleData.put("numReconsFromStep", numReconsFromStep);
            teleData.put("scoutName", getIntent().getStringExtra("scoutName"));
            teleData.put("coopActions", coopActions);

            ArrayList<Object> autoDataList = (ArrayList<Object>)intent.getSerializableExtra("data");
            Map<String, Object> autoData = (Map<String, Object>)autoDataList.get(0);

            intent.removeExtra("data");

            ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
            data.add(autoData);
            data.add(teleData);

            int teamNumber = intent.getIntExtra("teamNumber", 0);
            String match = intent.getStringExtra("matchNumber");

            String jsonString = createChangePacket(teamNumber, match, data);

			writeToFile(
                    jsonString,
                    intent.getStringExtra("matchNumber")
                            + "_"
                            + ((Integer) intent.getIntExtra("teamNumber", 0))
                            .toString() + ".json");

			uploadBluetooth(jsonString);

            Log.e("test", "The change packet is " + jsonString);

			Log.e("woo", Boolean.toString(shouldUpload));

			Intent menuIntent = new Intent(this, MainActivity.class);
			menuIntent.putExtra("submit", "yes");
            menuIntent.putExtra("json", jsonString);

			setResult(RESULT_OK, menuIntent);
			finish();
		}
		return true;
	}

    public String createChangePacket(int teamNumber, String match, List<Map<String, Object>> data) {

        JSONObject json = new JSONObject();
        try {
            json.put("uniqueValue", teamNumber);
            json.put("class", "Team");
            json.put("scoutName", data.get(1).get("scoutName"));
            json.put("allianceColor", MainActivity.isRed() ? "red" : "blue");

            JSONArray changes = new JSONArray();
            Map<String, Object> autoData = data.get(0);
            for (String key : autoData.keySet()) {
                if (!key.equals("reconAcquisitions")) {
                    JSONObject change = new JSONObject();
                    change.put("keyToChange", "matchData." + match + ".uploadedData." + key);
                    change.put("valueToChangeTo", autoData.get(key));
                    changes.put(change);
                } else {
                    int id = 0;
                    for (Map<String, Object> reconAcquisition : (List<Map<String, Object>>)autoData.get(key)) {
                        int index = ((List) autoData.get(key)).indexOf(reconAcquisition);
                        for (String reconKey : reconAcquisition.keySet()) {
                            JSONObject change = new JSONObject();
                            change.put("keyToChange", "matchData." + match + ".uploadedData.reconAcquisitions." + id + "." + reconKey);
                            change.put("valueToChangeTo", reconAcquisition.get(reconKey));
                            changes.put(change);
                        }
                        id++;
                    }
                    Log.e("test", "RA");
                }
            }

            Map<String, Object> teleData = data.get(1);
            for (String key : teleData.keySet()) {
                if (key != "coopActions") {
                    if (key != "scoutName") {
                        JSONObject change = new JSONObject();
                        change.put("keyToChange", "matchData." + match + ".uploadedData." + key);
                        change.put("valueToChangeTo", teleData.get(key));
                        changes.put(change);
                    }
                } else {
                    for (Map<String, Object> coopAction : (List<Map<String, Object>>)teleData.get(key)) {
                        int index = ((List) teleData.get(key)).indexOf(coopAction);
                        int id = (MainActivity.scoutID * 10) + index;
                        for (String coopKey : coopAction.keySet()) {
                            JSONObject change = new JSONObject();
                            change.put("keyToChange", "matchData." + match + ".uploadedData.coopActions." + id + "." + coopKey);
                            change.put("valueToChangeTo", coopAction.get(coopKey));
                            changes.put(change);
                        }
                    }
                }
            }

            json.put("changes", changes);
        } catch (JSONException jsone) {
            Log.e("test", jsone.getMessage());
        }

        return json.toString();
    }


    public void uploadBluetooth(String jsonString)
	{

			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter

					.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				// Device does not support Bluetooth
				Toaster.makeErrorToast("Bluetooth not connected",
						Toast.LENGTH_LONG);
			}

			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
					.getBondedDevices();
			if (pairedDevices.size() > 0) {
				for (BluetoothDevice device : pairedDevices) {
					ConnectThread connectThread = new ConnectThread(device,
							mBluetoothAdapter, jsonString, this, null, 5,
							true);
					connectThread.start();
				}
			}
	}

	public void onBluetoothFinish(boolean b) {
        Log.e("test", "Did it succeed? " + b);
		Log.e("sfdsfs", "Bluetooth finish");
	}
}
