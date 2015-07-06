/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.example.android.bluetoothchat;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase{

    public static final String TAG = "MainActivity";

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;
    private ListView musiclist;
    private ArrayAdapter<String> adMusicList;

    private String[][] chrildrenAddr = new String[1024][256];//folder information
    private String[][] fileArr = new String[1024][256] ;//file information
    private int folderCounter = 0;
    private String[] foldermem = new String[1024];
    private String[] parentID = new String[1024];
    private int levelCounter = 0;
    private int numChrildren = 0;
    private int numFile = 0;
    private String[][] folder = new String[1024][1024];
    private String[][] file = new String[1024][1034];// PID(parent ID), start index, end index
    private ArrayList<String> myFolderList = new ArrayList<String>();
    private ArrayList<String> myFolderIDList = new ArrayList<String>();
    private ArrayList<String> myFileList = new ArrayList<String>();
    private ArrayList<String> myFileIDList = new ArrayList<String>();
    private ArrayList<String> myPIDMemList = new ArrayList<String>();
    private ArrayList<String> myPIDMemIdList = new ArrayList<String>();


    private BluetoothChatFragment fragment;
    private String idcommand = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            fragment = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }

        musiclist = (ListView) findViewById(R.id.listview);

        JSONDecode(test2, 5, levelCounter, "-1");

        for (int p =0; p < numChrildren;p++){
            String pid = folder[p][0] ;
            initPIDfromChrildren(pid, p);
        }
        initPIDfromFile();
        getPIDAllMemNameAndId("-1"); //root PID is "-1"


//        adMusicList = ArrayAdapter.createFromResource(this, R.array.weekday,
//                        android.R.layout.simple_list_item_1);
        adMusicList = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,myPIDMemList);

        musiclist.setAdapter(adMusicList);
        musiclist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String id_here = myPIDMemIdList.get(position);
                idcommand = "ID_" + id_here + "\r\n";
                Toast.makeText(MainActivity.this, "id_here:" + id_here, Toast.LENGTH_LONG).show();
                getPIDAllMemNameAndId(id_here);
                adMusicList.notifyDataSetChanged();
//                String track = ((TextView) view).getText().toString();
//                Toast.makeText(MainActivity.this, myPIDMemList.get(0), Toast.LENGTH_LONG).show();
            }

//            Bundle bundle = new Bundle();
//            bundle.putString();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // level記錄第幾層，從零層開始 JSONDecode(test, 5, levelCounter=0,-1");
    private void JSONDecode(String child, int x, int level,String parentId) {
        try{
            JSONObject jsonObj = new JSONObject(child);
            if(jsonObj.getString("type").equals("folder")){
                chrildrenAddr[numChrildren][0] = jsonObj.getString("id");
                chrildrenAddr[numChrildren][1] = jsonObj.getString("type");
                chrildrenAddr[numChrildren][2] = jsonObj.getString("name");
                chrildrenAddr[numChrildren][3] = String.valueOf(levelCounter);  //the level of the folder
                chrildrenAddr[numChrildren][4] = parentId;
                folder[numChrildren][0] = parentId;
                file[numChrildren][0] = jsonObj.getString("id");

                int i = x;
                // 沒有子資料夾的話
                if( !jsonObj.getString("children").equals(null)) {
                    JSONArray jsonArray = new JSONArray(jsonObj.getString("children"));
                    foldermem[folderCounter] = String.valueOf(levelCounter) ;  //level of each child
                    folderCounter++;
                    numChrildren++;

                    for (int j = 0; j < jsonArray.length(); j++) {
                        if(j == 0){
                            i = 5;
                            levelCounter = level+1;
                        }
                        JSONObject jsonObject = jsonArray.getJSONObject(j);
                        chrildrenAddr[numChrildren-1][i] = jsonObject.toString();
                        JSONDecode(jsonObject.toString(), i, levelCounter, jsonObj.getString("id"));
                        i++;
                        if(j == (jsonArray.length() -1) ){
                            levelCounter = level-1;
                        }
                    }
                }
            }

            else if(jsonObj.getString("type").equals("file")){
                fileArr[numFile][0] = jsonObj.getString("album");
                fileArr[numFile][1] = jsonObj.getString("artist");
                fileArr[numFile][2] = jsonObj.getString("format");
                fileArr[numFile][3] = jsonObj.getString("playtime");
                fileArr[numFile][4] = jsonObj.getString("type");
                fileArr[numFile][5] = jsonObj.getString("name");
                fileArr[numFile][6] = jsonObj.getString("id");
                fileArr[numFile][7] = jsonObj.getString("name") +  jsonObj.getString("format");//for printing to listview
                fileArr[numFile][8] = parentId;
                parentID[numFile] = parentId;
                numFile++;
            }

        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    /** Create a chain of targets that will receive log data */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }
    private void getPIDAllMemNameAndId(String pid){
        int index = findPIDfromfolder(pid);
        int index2 = findPIDfromfile(pid);
        if((index!= -1) | (index2 !=-1)){
            myPIDMemList.clear();
            myPIDMemIdList.clear();
            if (index != -1) {
                for (int i = 1; i < numChrildren; i++) {
                    if (folder[index][i] != null) {
                        myPIDMemList.add(chrildrenAddr[Integer.valueOf(folder[index][i])][2]);
                        myPIDMemIdList.add(chrildrenAddr[Integer.valueOf(folder[index][i])][0]);
                    }
                }
            }

            if (index2 != -1) {
                for (int i = 1; i < numFile; i++) {
                    if (file[index2][i] != null) {
                        myPIDMemList.add(fileArr[Integer.valueOf(file[index2][i])][7]);
                        myPIDMemIdList.add(fileArr[Integer.valueOf(file[index2][i])][6]);
                    }
                }
            }
        }
    }

    //return folder index of the PID
    private int findPIDfromfolder(String pid){
        for(int i = 0; i < numChrildren ; i++){
            if( folder[i][0].equals(pid)){
                return i;
            }
        }
        return -1;
    }

    //return file index of the PID
    private int findPIDfromfile(String pid){
        for(int i = 0; i < numChrildren ; i++){
            if( file[i][0].equals(pid)){
                return i;
            }
        }
        return -1;
    }


    private void initPIDfromChrildren(String parenId, int indexx){
        int folderLastIndex = 1;

        for (int i = 0; i<numChrildren; i++) {
            if(chrildrenAddr[i][4].equals(null))
                break;
            if(chrildrenAddr[i][4].equals( parenId)){
                folder[indexx][folderLastIndex] = String.valueOf(i);
                folderLastIndex++;
            }
        }
    }

    private void initPIDfromFile(){

        for(int i = 0;i< numChrildren;i++) {
            int fileLastIndex = 1;
            for(int j = 0; j<numFile;j++) {
                if (file[i][0].equals(fileArr[j][8])) {
                    file[i][fileLastIndex] = String.valueOf(j);
                    fileLastIndex++;
                }
            }
        }

    }
    private String test = "{\"children\": [{\"children\": [{\"children\": [{\"album\": \"Les Miserables\",\"name\": \"01. Look Down.mp3\",\"artist\": \"Hugh Jackman, Russell Crowe & Convicts\",\"format\": \".mp3\",\"playtime\": 141,\"type\": \"file\",\"id\": 3},{\"album\": \"Les Miserables\",\"name\": \"02. The Bishop.mp3\",\"artist\": \"Hugh Jackman & Colm Wilkinson\",\"format\": \".mp3\",\"playtime\": 95,\"type\": \"file\",\"id\": 4},{\"album\": \"Les Miserables\",\"name\": \"03. Valjean's Soliloquy.mp3\",\"artist\": \"Hugh Jackman\",\"format\": \".mp3\",\"playtime\": 199,\"type\": \"file\",\"id\": 5},{\"album\": \"Les Miserables\",\"name\": \"04. At The End Of The Day.mp3\",\"artist\": \"Hugh Jackman, Anne Hathaway, Factory Girls & Cast\",\"format\": \".mp3\",\"playtime\": 267,\"type\": \"file\",\"id\": 6},{\"album\": \"Les Miserables\",\"name\": \"05. I Dreamed A Dream.mp3\",\"artist\": \"Anne Hathaway\",\"format\": \".mp3\",\"playtime\": 278,\"type\": \"file\",\"id\": 7},{\"album\": \"Les Miserables\",\"name\": \"06. The Confrontation.mp3\",\"artist\": \"Hugh Jackman & Russell Crowe\",\"format\": \".mp3\",\"playtime\": 115,\"type\": \"file\",\"id\": 8},{\"album\": \"Les Miserables\",\"name\": \"07. Castle On A Cloud.mp3\",\"artist\": \"Isabelle Allen\",\"format\": \".mp3\",\"playtime\": 71,\"type\": \"file\",\"id\": 9},{\"album\": \"Les Miserables\",\"name\": \"08. Master Of The House.mp3\",\"artist\": \"Sacha Baron Cohen, Helena Bonham Carter & Cast\",\"format\": \".mp3\",\"playtime\": 292,\"type\": \"file\",\"id\": 10},{\"album\": \"Les Miserables\",\"name\": \"09. Suddenly.mp3\",\"artist\": \"Hugh Jackman\",\"format\": \".mp3\",\"playtime\": 152,\"type\": \"file\",\"id\": 11},{\"album\": \"Les Miserables\",\"name\": \"10. Stars.mp3\",\"artist\": \"Russell Crowe\",\"format\": \".mp3\",\"playtime\": 181,\"type\": \"file\",\"id\": 12},{\"album\": \"Les Miserables\",\"name\": \"11. ABC Cafe-Red & Black.mp3\",\"artist\": \"Eddie Redmayne, Aaron Tveit & Students\",\"format\": \".mp3\",\"playtime\": 262,\"type\": \"file\",\"id\": 13},{\"album\": \"Les Miserables\",\"name\": \"12. In My Life-A Heart Full Of Love.mp3\",\"artist\": \"Amanda Seyfried, Eddie Redmayne & Samantha Barks\",\"format\": \".mp3\",\"playtime\": 192,\"type\": \"file\",\"id\": 14},{\"album\": \"Les Miserables\",\"name\": \"13. On My Own.mp3\",\"artist\": \"Samantha Barks\",\"format\": \".mp3\",\"playtime\": 191,\"type\": \"file\",\"id\": 15},{\"album\": \"Les Miserables\",\"name\": \"14. One Day More.mp3\",\"artist\": \"Cast Of Les Miserables\",\"format\": \".mp3\",\"playtime\": 219,\"type\": \"file\",\"id\": 16},{\"album\": \"Les Miserables\",\"name\": \"15. Drink With Me.mp3\",\"artist\": \"Eddie Redmayne, Daniel Huttlestone & Students\",\"format\": \".mp3\",\"playtime\": 102,\"type\": \"file\",\"id\": 17},{\"album\": \"Les Miserables\",\"name\": \"16. Bring Him Home.mp3\",\"artist\": \"Hugh Jackman\",\"format\": \".mp3\",\"playtime\": 217,\"type\": \"file\",\"id\": 18},{\"album\": \"Les Miserables\",\"name\": \"17. The Final Battle.mp3\",\"artist\": \"Students & Cast Of Les Miserables\",\"format\": \".mp3\",\"playtime\": 197,\"type\": \"file\",\"id\": 19},{\"album\": \"Les Miserables\",\"name\": \"18. Javert's Suicide.mp3\",\"artist\": \"Russell Crowe\",\"format\": \".mp3\",\"playtime\": 180,\"type\": \"file\",\"id\": 20},{\"album\": \"Les Miserables\",\"name\": \"19. Empty Chairs At Empty Tables.mp3\",\"artist\": \"Eddie Redmayne\",\"format\": \".mp3\",\"playtime\": 193,\"type\": \"file\",\"id\": 21},{\"album\": \"Les Miserables\",\"name\": \"20. Epilogue.mp3\",\"artist\": \"Cast Of Les Miserables\",\"format\": \".mp3\",\"playtime\": 380,\"type\": \"file\",\"id\": 22}],\"type\": \"folder\",\"name\": \"Les Miserables\",\"id\": 2},{\"format\": \"notaudio\",\"type\": \"file\",\"name\": \"04.NIRGILIS - sakura.wav\",\"id\": 23},{\"children\": [{\"children\": [{\"children\": [{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"01. My Dear Frodo.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 483,\"type\": \"file\",\"id\": 27},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"02. Old Friends (Extended Version).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 300,\"type\": \"file\",\"id\": 28},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"03. An Unexpected Party (Extended Version).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 250,\"type\": \"file\",\"id\": 29},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"04. Blunt the Knives (Exclusive Bonus Track).mp3\",\"artist\": \"The Dwarf Cast\",\"format\": \".mp3\",\"playtime\": 61,\"type\": \"file\",\"id\": 30},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"05. Axe or Sword.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 359,\"type\": \"file\",\"id\": 31},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"06. Misty Mountains.mp3\",\"artist\": \"The Dwarf Cast and Richard Armitage\",\"format\": \".mp3\",\"playtime\": 102,\"type\": \"file\",\"id\": 32},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"07. The Adventure Begins.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 125,\"type\": \"file\",\"id\": 33},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"08. The World Is Ahead.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 141,\"type\": \"file\",\"id\": 34},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"09. An Ancient Enemy.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 298,\"type\": \"file\",\"id\": 35},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"10. Radagast the Brown (Extended Version).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 399,\"type\": \"file\",\"id\": 36},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"11. The Trollshaws (Exclusive Bonus Track).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 129,\"type\": \"file\",\"id\": 37},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"12. Roast Mutton (Extended Version).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 298,\"type\": \"file\",\"id\": 38},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"13. A Troll-Hoard.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 159,\"type\": \"file\",\"id\": 39},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"14. The Hill of Sorcery.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 231,\"type\": \"file\",\"id\": 40},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"15. Warg-Scouts.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 185,\"type\": \"file\",\"id\": 41}],\"type\": \"folder\",\"name\": \"CD1\",\"id\": 26},{\"children\": [{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"01. The Hidden Valley.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 229,\"type\": \"file\",\"id\": 43},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"02. Moon Runes (Extended Version).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 218,\"type\": \"file\",\"id\": 44},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"03. The Defiler.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 74,\"type\": \"file\",\"id\": 45},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"04. The White Council (Extended Version).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 582,\"type\": \"file\",\"id\": 46},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"05. Over Hill.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 223,\"type\": \"file\",\"id\": 47},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"06. A Thunder Battle.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 236,\"type\": \"file\",\"id\": 48},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"07. Under Hill.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 114,\"type\": \"file\",\"id\": 49},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"08. Riddles in the Dark.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 323,\"type\": \"file\",\"id\": 50},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"09. Brass Buttons.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 458,\"type\": \"file\",\"id\": 51},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"10. Out of the Frying-Pan.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 355,\"type\": \"file\",\"id\": 52},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"11. A Good Omen.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 347,\"type\": \"file\",\"id\": 53},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"12. Song of the Lonely Mountain (Extended Version).mp3\",\"artist\": \"Neil Finn\",\"format\": \".mp3\",\"playtime\": 363,\"type\": \"file\",\"id\": 54},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"13. Dreaming of Bag End.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 109,\"type\": \"file\",\"id\": 55},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"14. A Very Respectable Hobbit (Exclusive Bonus Track).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 87,\"type\": \"file\",\"id\": 56},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"15. Erebor (Exclusive Bonus Track).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 79,\"type\": \"file\",\"id\": 57},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"16. The Dwarf Lords (Exclusive Bonus Track).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 120,\"type\": \"file\",\"id\": 58},{\"album\": \"The Hobbit An Unexpected Journey\",\"name\": \"17. The Edge of the Wild (Exclusive Bonus Track).mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 218,\"type\": \"file\",\"id\": 59}],\"type\": \"folder\",\"name\": \"CD2\",\"id\": 42}],\"type\": \"folder\",\"name\": \"The Hobbit An Unexpected Journey\",\"id\": 25},{\"children\": [{\"album\": null,\"name\": \"1 Movement One.mp3\",\"artist\": null,\"format\": \".mp3\",\"playtime\": 685,\"type\": \"file\",\"id\": 61},{\"album\": null,\"name\": \"2 Movement Two.mp3\",\"artist\": null,\"format\": \".mp3\",\"playtime\": 2044,\"type\": \"file\",\"id\": 62},{\"album\": \"The Lord Of The Rings Symphony [Disc 2]\",\"name\": \"3 Movement Three.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 1095,\"type\": \"file\",\"id\": 63},{\"album\": \"The Lord Of The Rings Symphony [Disc 2]\",\"name\": \"4 Movement Four.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 628,\"type\": \"file\",\"id\": 64},{\"album\": \"The Lord Of The Rings Symphony [Disc 2]\",\"name\": \"5 Movement Five.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 926,\"type\": \"file\",\"id\": 65},{\"album\": \"The Lord Of The Rings Symphony [Disc 2]\",\"name\": \"6 Movement Six.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 1572,\"type\": \"file\",\"id\": 66},{\"format\": \"notaudio\",\"type\": \"file\",\"name\": \"AlbumArtSmall.jpg\",\"id\": 67},{\"format\": \"notaudio\",\"type\": \"file\",\"name\": \"Folder.jpg\",\"id\": 68}],\"type\": \"folder\",\"name\": \"The Lord of the Rings Symphony\",\"id\": 60},{\"children\": [{\"format\": \"notaudio\",\"type\": \"file\",\"name\": \"Thumbs.db\",\"id\": 70},{\"children\": [{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"01 - The Fellowship.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 352,\"type\": \"file\",\"id\": 72},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"02 - The Prophecy.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 301,\"type\": \"file\",\"id\": 73},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"03 - Concerning Hobbits.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 164,\"type\": \"file\",\"id\": 74},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"04 - The Shadow of the Past - A Knife in the Dark.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 446,\"type\": \"file\",\"id\": 75},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"05 - The Bridge of Khazad Dum.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 358,\"type\": \"file\",\"id\": 76},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"06 - May It Be (Instrumental Version).mp3\",\"artist\": \"Enya\",\"format\": \".mp3\",\"playtime\": 319,\"type\": \"file\",\"id\": 77},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"07 - The Riders of Rohan.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 232,\"type\": \"file\",\"id\": 78},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"08 - Evenstar.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 218,\"type\": \"file\",\"id\": 79},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"09 - Forth Eorlingas - Isengard Unleashed.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 510,\"type\": \"file\",\"id\": 80},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"10 - Gollum's Song (Instrumental Version).mp3\",\"artist\": \"Howard Shore & Fran Walsh\",\"format\": \".mp3\",\"playtime\": 348,\"type\": \"file\",\"id\": 81}],\"type\": \"folder\",\"name\": \"Disc 1\",\"id\": 71},{\"children\": [{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"01 - Hope and Memory - Minas Tirith.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 158,\"type\": \"file\",\"id\": 83},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"02 - The White Tree.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 227,\"type\": \"file\",\"id\": 84},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"03 - Twilight and Shadow.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 233,\"type\": \"file\",\"id\": 85},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"04 - The Fields of Pelennor.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 192,\"type\": \"file\",\"id\": 86},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"05 - The Return of the King - Finale.mp3\",\"artist\": \"Howard Shore\",\"format\": \".mp3\",\"playtime\": 977,\"type\": \"file\",\"id\": 87},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"06 - Into the West (Instrumental Version).mp3\",\"artist\": \"Howard Shore, Fran Walsh & Annie Lennox\",\"format\": \".mp3\",\"playtime\": 280,\"type\": \"file\",\"id\": 88},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"07 - Gollum's Song (Performed by Helen Hobson).mp3\",\"artist\": \"Helen Hobson\",\"format\": \".mp3\",\"playtime\": 245,\"type\": \"file\",\"id\": 89},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"08 - May It Be (Performed by Tara Scammell).mp3\",\"artist\": \"Tara Scammell\",\"format\": \".mp3\",\"playtime\": 286,\"type\": \"file\",\"id\": 90},{\"album\": \"The Lord of the Rings Trilogy\",\"name\": \"09 - Into the West (Performed by Helen Hobson).mp3\",\"artist\": \"Helen Hobson\",\"format\": \".mp3\",\"playtime\": 280,\"type\": \"file\",\"id\": 91}],\"type\": \"folder\",\"name\": \"Disc 2\",\"id\": 82}],\"type\": \"folder\",\"name\": \"The Lord of the Rings Trilogy\",\"id\": 69}],\"type\": \"folder\",\"name\": \"Howard Shore\",\"id\": 24}],\"type\": \"folder\",\"name\": \"music\",\"id\": 1}],\"type\": \"folder\",\"name\": \"CHARLES_16G\",\"id\": 0}";

    private String test2="{\"children\": [{\"children\": [{\"children\": [{\"format\": \"notaudio\", \"type\": \"file\", \"name\": \"AlbumArtSmall.jpg\", \"id\": 3}, {\"format\": \"notaudio\", \"type\": \"file\", \"name\": \"Folder.jpg\", \"id\": 4}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"Intro ~\\u672a\\u6765~ - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 67, \"type\": \"file\", \"id\": 5}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u30a8\\u30f3\\u30b8\\u30a7\\u30eb\\u306e\\u8bd7 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 74, \"type\": \"file\", \"id\": 6}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u30ad\\u30df\\u3068\\u306e\\u7ea6\\u675f - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 44, \"type\": \"file\", \"id\": 7}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u4e9a\\u7279\\u5170\\u63d0\\u65af\\u4e4b\\u604b - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 354, \"type\": \"file\", \"id\": 8}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u5411\\u65e5\\u8475\\u7684\\u7948\\u613f - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 260, \"type\": \"file\", \"id\": 9}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u5730\\u7403\\u4e4b\\u6cea - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 372, \"type\": \"file\", \"id\": 10}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u5b88\\u62a4\\u5929\\u4f7f - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 307, \"type\": \"file\", \"id\": 11}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u5c0f\\u661f\\u661f\\u5e7b\\u60f3\\u66f2 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 200, \"type\": \"file\", \"id\": 12}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u601d\\u3044\\u51fa\\u306e\\u7bb1 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 60, \"type\": \"file\", \"id\": 13}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u65e9\\u6668\\u7684\\u7b2c\\u4e00\\u9053\\u5149\\u8292\\u4e0e\\u4f60\\u7684\\u5fae\\u7b11 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 248, \"type\": \"file\", \"id\": 14}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u6cea\\u7684\\u58f0\\u97f3 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 323, \"type\": \"file\", \"id\": 15}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u7231\\u2027\\u65e0\\u9650 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 227, \"type\": \"file\", \"id\": 16}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u7434\\u4e4b\\u7ffc - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 334, \"type\": \"file\", \"id\": 17}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u82b1\\u6c34\\u6708 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 166, \"type\": \"file\", \"id\": 18}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u8309\\u8389\\u2027\\u60f3\\u5ff5 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 326, \"type\": \"file\", \"id\": 19}, {\"album\": \"\\u7231\\u2027\\u65e0\\u9650\", \"name\": \"\\u98ce\\u306e\\u75d5\\u8ff9 - V.K\\u514b.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 332, \"type\": \"file\", \"id\": 20}], \"type\": \"folder\", \"name\": \"\\u7231\\u2027\\u65e0\\u9650\", \"id\": 2}, {\"children\": [{\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"01-1.INTRO VENLS.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 34, \"type\": \"file\", \"id\": 22}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"02-2.\\u955c\\u591c.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 276, \"type\": \"file\", \"id\": 23}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"03-3.\\u604b\\u00b7\\u51ac.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 295, \"type\": \"file\", \"id\": 24}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"04-4.\\u9999\\u8349\\u6ce1\\u6ce1\\u7684\\u5b63\\u8282.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 339, \"type\": \"file\", \"id\": 25}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"05-5.EPISODE I ~ \\u79c1\\u305f\\u3061\\u306f\\u5e78\\u798f\\u304c\\u8981\\u308b\\u2026.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 107, \"type\": \"file\", \"id\": 26}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"06-6.\\u72ee\\u5b50\\u5927\\u5f20\\u5634.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 200, \"type\": \"file\", \"id\": 27}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"07-7.\\u6d77\\u6d0b\\u4e4b\\u606f.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 261, \"type\": \"file\", \"id\": 28}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"08-8.\\u62b9\\u8336\\u62ff\\u94c1.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 301, \"type\": \"file\", \"id\": 29}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"09-9.EPISODE II ~\\u96ea\\u306e\\u821e~ \\u3000.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 77, \"type\": \"file\", \"id\": 30}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"10-10.\\u7eaf\\u767d.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 340, \"type\": \"file\", \"id\": 31}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"11-11.\\u65cb\\u8f6c\\u6728\\u9a6c\\u524d\\u7684\\u7ea6\\u5b9a.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 182, \"type\": \"file\", \"id\": 32}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"12-12.\\u7cbe\\u7075\\u4e4b\\u6b4c.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 372, \"type\": \"file\", \"id\": 33}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"13-13.\\u6668\\u661f.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 251, \"type\": \"file\", \"id\": 34}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"14-14.FINAL ~ \\u5e78\\u305b\\u306a\\u7ea6\\u675f\\u3060~.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 50, \"type\": \"file\", \"id\": 35}, {\"album\": \"\\u00c3\\u00e8\\u00a9]\", \"name\": \"15-15.\\u7eef\\u6a31.mp3\", \"artist\": \"V.K\\u00a7J\", \"format\": \".mp3\", \"playtime\": 329, \"type\": \"file\", \"id\": 36}], \"type\": \"folder\", \"name\": \"\\u955c\\u591c\", \"id\": 21}], \"type\": \"folder\", \"name\": \"V.K\\u514b\", \"id\": 1}, {\"children\": [{\"children\": [{\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"01.\\u6211\\u662f\\u4e00\\u96bb\\u5c0f\\u5c0f\\u9ce5.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 289, \"type\": \"file\", \"id\": 39}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"02.\\u98db\\u6a5f\\u5834\\u768410\\uff1a30.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 275, \"type\": \"file\", \"id\": 40}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"03.Kiss Goodbye.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 250, \"type\": \"file\", \"id\": 41}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"04.\\u5206\\u624b\\u5427.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 257, \"type\": \"file\", \"id\": 42}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"05.\\u6d0b\\u8525.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 306, \"type\": \"file\", \"id\": 43}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"06.\\u98c4\\u6d0b\\u904e\\u6d77\\u4f86\\u770b\\u4f60.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 267, \"type\": \"file\", \"id\": 44}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"07.\\u4e0d\\u53ea\\u662f\\u670b\\u53cb.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 289, \"type\": \"file\", \"id\": 45}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"08.Crazy In Love.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 240, \"type\": \"file\", \"id\": 46}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"09.\\u5927\\u6d77.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 279, \"type\": \"file\", \"id\": 47}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"10.I Will Always Love You.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 268, \"type\": \"file\", \"id\": 48}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"01.Fu Good (\\u4e2d\\u5929\\u97d3\\u5287\\u300e\\u55ae\\u8eab\\u7238\\u7238\\u6200\\u611b\\u4e2d\\u300f\\u7247\\u982d\\u66f2).mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 204, \"type\": \"file\", \"id\": 49}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"02.\\u731c\\u4e0d\\u900f.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 231, \"type\": \"file\", \"id\": 50}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"03.\\u6211\\u611b\\u4ed6.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 281, \"type\": \"file\", \"id\": 51}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"04.\\u4f60\\u70ba\\u4ec0\\u9ebc\\u8aaa\\u8b0a.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 271, \"type\": \"file\", \"id\": 52}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"05.\\u53ef\\u4ee5\\u4e0d\\u53ef\\u4ee5.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 280, \"type\": \"file\", \"id\": 53}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"06.\\u6211\\u4e0d\\u6015 (vs.MC Hot Dog).mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 225, \"type\": \"file\", \"id\": 54}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"07.\\u96e2\\u5bb6\\u51fa\\u8d70.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 292, \"type\": \"file\", \"id\": 55}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"08.\\u660e\\u767d.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 239, \"type\": \"file\", \"id\": 56}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"09.\\u82b1\\u706b (vs.\\u4e94\\u6708\\u5929\\u963f\\u4fe1).mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 246, \"type\": \"file\", \"id\": 57}, {\"album\": \"\\u00a4B\\u00be\\u00b4 - Fu Good \\u00a4U\\u00a4@\\u00af\\u00b8 \\u00a4\\u00d1\\u00a6Z (\\u00a6\\u00db\\u00bf\\u00ef+\\u00ba\\u00eb\\u00bf\\u00ef)\", \"name\": \"10.\\u6211\\u611b\\u4e0a\\u7684.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 245, \"type\": \"file\", \"id\": 58}], \"type\": \"folder\", \"name\": \"Fu Good \\u4e0b\\u4e00\\u7ad9 \\u5929\\u540e\", \"id\": 38}, {\"children\": [{\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"01.\\u82b1\\u706b - \\u4e01\\u5679 + \\u4e94\\u6708\\u5929\\u963f\\u4fe1.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 246, \"type\": \"file\", \"id\": 60}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"02.\\u6211\\u611b\\u4ed6.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 282, \"type\": \"file\", \"id\": 61}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"03.\\u4f60\\u70ba\\u4ec0\\u9ebc\\u8aaa\\u8b0a.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 273, \"type\": \"file\", \"id\": 62}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"04.\\u591c\\u8c93.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 208, \"type\": \"file\", \"id\": 63}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"05.\\u7a81\\u7136\\u60f3\\u611b\\u4f60 - \\u4e01\\u5679 + \\u5468\\u83ef\\u5065.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 294, \"type\": \"file\", \"id\": 64}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"06.\\u4ed6\\u8aaa\\u4f60\\u6c92\\u7528 (Rap - Magic Power).mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 217, \"type\": \"file\", \"id\": 65}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"07.\\u5168\\u4e16\\u754c\\u4e0d\\u61c2\\u7121\\u6240\\u8b02.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 197, \"type\": \"file\", \"id\": 66}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"08 \\u89aa\\u4eba.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 291, \"type\": \"file\", \"id\": 67}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"09 \\u534a\\u5e36\\u96fb.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 209, \"type\": \"file\", \"id\": 68}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"10 \\u53ef\\u60dc\\u4e86.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 215, \"type\": \"file\", \"id\": 69}, {\"album\": \"\\u00a9]\\u00bf\\u00df\", \"name\": \"11 \\u9577\\u5927.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 288, \"type\": \"file\", \"id\": 70}], \"type\": \"folder\", \"name\": \"\\u591c\\u8c93\", \"id\": 59}, {\"children\": [{\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"01.\\u6211\\u611b\\u4e0a\\u7684.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 246, \"type\": \"file\", \"id\": 72}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"02.\\u5e78\\u904b\\u8349.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 264, \"type\": \"file\", \"id\": 73}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"03.\\u9583\\u5149\\u71c8.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 234, \"type\": \"file\", \"id\": 74}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"04.\\u800d\\u5927\\u724c.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 252, \"type\": \"file\", \"id\": 75}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"05.\\u731c\\u4e0d\\u900f.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 233, \"type\": \"file\", \"id\": 76}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"06.\\u6211\\u4e0d\\u6015.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 227, \"type\": \"file\", \"id\": 77}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"07.\\u8d70\\u706b\\u5165\\u9b54 (\\u4e01\\u5679+\\u963f\\u4fe1).mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 283, \"type\": \"file\", \"id\": 78}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"08.\\u860b\\u679c\\u5149.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 255, \"type\": \"file\", \"id\": 79}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"09.\\u60f3\\u5f97\\u7f8e.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 222, \"type\": \"file\", \"id\": 80}, {\"album\": \"\\u00a7\\u00da\\u00b7R\\u00a4W\\u00aa\\u00ba\", \"name\": \"10.\\u77f3\\u982d.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 259, \"type\": \"file\", \"id\": 81}], \"type\": \"folder\", \"name\": \"\\u6211\\u611b\\u4e0a\\u7684\", \"id\": 71}, {\"children\": [{\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"01. \\u51b7\\u8840\\u52d5\\u7269.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 252, \"type\": \"file\", \"id\": 83}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"02. \\u4e00\\u534a.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 268, \"type\": \"file\", \"id\": 84}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"03. \\u672a\\u4f86\\u7684\\u60c5\\u4eba.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 235, \"type\": \"file\", \"id\": 85}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"04. \\u6700\\u5f8c\\u4e00\\u6b21\\u5bc2\\u5bde.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 302, \"type\": \"file\", \"id\": 86}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"05. \\u591a\\u611b\\u5c11\\u602a.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 207, \"type\": \"file\", \"id\": 87}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"06. \\u8e39\\u4f86\\u5171.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 237, \"type\": \"file\", \"id\": 88}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"07. \\u60f3\\u539f\\u8ad2.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 266, \"type\": \"file\", \"id\": 89}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"08. \\u5f88\\u611b\\u904e.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 217, \"type\": \"file\", \"id\": 90}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"09. SYMPHONY.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 40, \"type\": \"file\", \"id\": 91}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"10. \\u5922\\u4ea4\\u97ff.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 272, \"type\": \"file\", \"id\": 92}, {\"album\": \"\\u00a5\\u00bc\\u00a8\\u00d3\\u00aa\\u00ba\\u00b1\\u00a1\\u00a4H\", \"name\": \"11. BACK-UP.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 205, \"type\": \"file\", \"id\": 93}], \"type\": \"folder\", \"name\": \"\\u672a\\u4f86\\u7684\\u60c5\\u4eba\", \"id\": 82}, {\"children\": [{\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"01. \\u96e2\\u5bb6\\u51fa\\u8d70.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 294, \"type\": \"file\", \"id\": 95}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"02. \\u53ef\\u4ee5\\u4e0d\\u53ef\\u4ee5.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 282, \"type\": \"file\", \"id\": 96}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"03. \\u660e\\u767d.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 241, \"type\": \"file\", \"id\": 97}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"04. \\u611b\\u60c5\\u6c92\\u6709\\u7b54\\u6848.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 265, \"type\": \"file\", \"id\": 98}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"05. \\u4ee3\\u66ff.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 329, \"type\": \"file\", \"id\": 99}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"06. \\u81ea\\u7531.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 252, \"type\": \"file\", \"id\": 100}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"07. \\u96d9\\u98db.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 303, \"type\": \"file\", \"id\": 101}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"08. \\u767d\\u86c7.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 302, \"type\": \"file\", \"id\": 102}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"09. \\u5f80\\u5fc3\\u7406\\u63a2\\u96aa.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 258, \"type\": \"file\", \"id\": 103}, {\"album\": \"\\u00c2\\u00f7\\u00aea\\u00a5X\\u00a8\\u00ab\", \"name\": \"10. \\u8a0e\\u4f60\\u6b61\\u5fc3.mp3\", \"artist\": \"\\u00a4B\\u00be\\u00b4\", \"format\": \".mp3\", \"playtime\": 273, \"type\": \"file\", \"id\": 104}], \"type\": \"folder\", \"name\": \"\\u96e2\\u5bb6\\u51fa\\u8d70\", \"id\": 94}], \"type\": \"folder\", \"name\": \"\\u4e01\\u5679\", \"id\": 37}, {\"children\": [{\"children\": [{\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"01 - High Priests.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 162, \"type\": \"file\", \"id\": 107}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"02 - Dance Of Love.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 159, \"type\": \"file\", \"id\": 108}, {\"album\": \"Michael Flatley's Feet Of Flam\", \"name\": \"03 - Carrickfergus.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 232, \"type\": \"file\", \"id\": 109}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"04 - Duelling Violins.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 224, \"type\": \"file\", \"id\": 110}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"05 - Whispering Wind.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 169, \"type\": \"file\", \"id\": 111}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"06 - Dance Above The Rainbow.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 181, \"type\": \"file\", \"id\": 112}, {\"album\": \"Michael Flatley's Feet Of Flam\", \"name\": \"07 - The Dawning.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 484, \"type\": \"file\", \"id\": 113}, {\"album\": \"Michael Flatley's Feet Of Flam\", \"name\": \"08 - Spirit's Lament.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 121, \"type\": \"file\", \"id\": 114}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"09 - I Dreamt I Dwelt.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 205, \"type\": \"file\", \"id\": 115}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"10 - Strings Of Fire.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 237, \"type\": \"file\", \"id\": 116}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"11 - Hell's Kitchen.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 176, \"type\": \"file\", \"id\": 117}, {\"album\": \"Michael Flatley's Feet Of Flames\", \"name\": \"12 - Celtic Fire (Live).mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 228, \"type\": \"file\", \"id\": 118}, {\"format\": \"notaudio\", \"type\": \"file\", \"name\": \"AlbumArtSmall.jpg\", \"id\": 119}, {\"format\": \"notaudio\", \"type\": \"file\", \"name\": \"Folder.jpg\", \"id\": 120}], \"type\": \"folder\", \"name\": \"Michael Flatley's Feet Of Flames\", \"id\": 106}, {\"children\": [{\"album\": \"Lord of The Dance\", \"name\": \"01 - Cry Of The Celts.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 264, \"type\": \"file\", \"id\": 122}, {\"album\": \"Lord of The Dance\", \"name\": \"02 - Suila A Ruin.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 194, \"type\": \"file\", \"id\": 123}, {\"album\": \"Lord of The Dance\", \"name\": \"03 - Celtic Dream.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 337, \"type\": \"file\", \"id\": 124}, {\"album\": \"Lord of The Dance\", \"name\": \"04 - Warriors.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 187, \"type\": \"file\", \"id\": 125}, {\"album\": \"Lord of The Dance\", \"name\": \"05 - Gypsy.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 128, \"type\": \"file\", \"id\": 126}, {\"album\": \"Lord of The Dance\", \"name\": \"06 - Breakout.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 226, \"type\": \"file\", \"id\": 127}, {\"album\": \"Lord of The Dance\", \"name\": \"07 - The Lord Of The Dance.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 283, \"type\": \"file\", \"id\": 128}, {\"album\": \"Lord of The Dance\", \"name\": \"08 - Spirit Of The New World.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 91, \"type\": \"file\", \"id\": 129}, {\"album\": \"Lord of The Dance\", \"name\": \"09 - Fiery Nights.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 189, \"type\": \"file\", \"id\": 130}, {\"album\": \"Lord of The Dance\", \"name\": \"10 - Lament.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 202, \"type\": \"file\", \"id\": 131}, {\"album\": \"Lord of The Dance\", \"name\": \"11 - Siamsa.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 267, \"type\": \"file\", \"id\": 132}, {\"album\": \"Lord of The Dance\", \"name\": \"12 - Our Wedding Day.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 203, \"type\": \"file\", \"id\": 133}, {\"album\": \"Lord of The Dance\", \"name\": \"13 - Stolen Kiss.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 205, \"type\": \"file\", \"id\": 134}, {\"album\": \"Lord of The Dance\", \"name\": \"14 - Nightmare.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 217, \"type\": \"file\", \"id\": 135}, {\"album\": \"Lord of The Dance\", \"name\": \"15 - Victory.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 164, \"type\": \"file\", \"id\": 136}, {\"album\": \"Lord of The Dance\", \"name\": \"16 - Cry Of The Celts.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 140, \"type\": \"file\", \"id\": 137}, {\"album\": \"Lord of The Dance\", \"name\": \"17 - The Lord Of The Dance.mp3\", \"artist\": \"Ronan Hardiman\", \"format\": \".mp3\", \"playtime\": 283, \"type\": \"file\", \"id\": 138}], \"type\": \"folder\", \"name\": \"Lord Of The Dance\", \"id\": 121}], \"type\": \"folder\", \"name\": \"Michael Flatley\", \"id\": 105}], \"type\": \"folder\", \"name\": \"CHARLES_16G\", \"id\": 0}";
}