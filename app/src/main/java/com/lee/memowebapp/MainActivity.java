package com.lee.memowebapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private EditText mEditText;
    private TextView mTxtName, mTxtEmail;
    private NavigationView mNavigationView;
    private String mSelectedMemoKey;
    private BackPressCloseHandler mBackPressCloseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        CheckTypesTask task = new CheckTypesTask();
        task.execute();
        mBackPressCloseHandler = new BackPressCloseHandler(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mEditText = (EditText) findViewById(R.id.content);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        if(mFirebaseUser == null) {
            startActivity(new Intent(MainActivity.this, AuthActivity.class));
            finish();//현재 보고 있는 창(인텐트)을 닫는다.
            return;
        }


        setSupportActionBar(toolbar);
        FloatingActionButton fabNewMemo = (FloatingActionButton) findViewById(R.id.new_memo);
        FloatingActionButton fabSaveMemo = (FloatingActionButton) findViewById(R.id.save_memo);

        fabNewMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initMemo();
            }
        });

        fabSaveMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSelectedMemoKey == null) {
                    saveMemo();
                } else {
                    updateMemo();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = mNavigationView.getHeaderView(0);
        mTxtName = (TextView) headerView.findViewById(R.id.txtName);
        mTxtEmail = (TextView) headerView.findViewById(R.id.txtEmail);
        mNavigationView.setNavigationItemSelectedListener(this);
        profileUpdate();
        displayMemos();
    }

    private void saveMemo() {
        String text = mEditText.getText().toString();
        if(text.isEmpty()) {
            Snackbar.make(mEditText, "메모를 입력해 주세요", Snackbar.LENGTH_LONG).show();
            return;
        }
        Memo memo = new Memo();
        memo.setTxt(mEditText.getText().toString());

        memo.setCreateDate(new Date().getTime());
        mFirebaseDatabase
                .getReference("memos/" + mFirebaseUser
                .getUid())
                .push()
                .setValue(memo)
                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(mEditText, "메모가 저장되었습니다", Snackbar.LENGTH_LONG).show();
                        initMemo();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            mBackPressCloseHandler.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            deleteMemo();
        } else if (id == R.id.action_logout) {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Memo selectedMemo = (Memo) item.getActionView().getTag();
        mEditText.setText(selectedMemo.getTxt());
        mSelectedMemoKey = selectedMemo.getKey();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void profileUpdate() {
        mTxtEmail.setText(mFirebaseUser.getEmail());
        mTxtName.setText(mFirebaseUser.getDisplayName());

    }

    private void displayMemos() {
        mFirebaseDatabase.getReference("memos/" + mFirebaseUser.getUid())
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Memo memo = dataSnapshot.getValue(Memo.class);
                        memo.setKey(dataSnapshot.getKey());
                        displayMemoList(memo);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        Memo memo = dataSnapshot.getValue(Memo.class);
                        memo.setKey(dataSnapshot.getKey());

                        for(int i = 0; i < mNavigationView.getMenu().size(); i++) {
                            MenuItem menuItem = mNavigationView.getMenu().getItem(i);
                            Memo memoObj = (Memo) menuItem.getActionView().getTag();
                            String key = memoObj.getKey();
                            if(memo.getKey().equals(key)) {
                                menuItem.getActionView().setTag(memo);
                                menuItem.setTitle(memo.getTitle());
                                menuItem.setTitle(memo.getTxt());
                            }
                        }
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Memo memo = dataSnapshot.getValue(Memo.class);
                        memo.setKey(dataSnapshot.getKey());
                        for(int i = 0; i < mNavigationView.getMenu().size(); i++) {
                            MenuItem menuItem = mNavigationView.getMenu().getItem(i);
                            Memo memoObj = (Memo) menuItem.getActionView().getTag();
                            String key = memoObj.getKey();
                            if(memo.getKey().equals(key)) {
                                menuItem.setVisible(false);
                            }
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }
    private void displayMemoList(Memo memo) {
        Menu leftMenu = mNavigationView.getMenu();
        MenuItem menuItem = leftMenu.add(memo.getTitle());
        View view = new View(getApplication());
        view.setTag(memo);
        menuItem.setActionView(view);
    }
    private void initMemo() {
        mSelectedMemoKey = null;
        mEditText.setText("");
    }

    private void updateMemo() {
        String text = mEditText.getText().toString();
        if(text.isEmpty()) {
            return;
        }
        Memo memo = new Memo();
        memo.setTxt(mEditText.getText().toString());
        memo.setCreateDate(new Date().getTime());
        mFirebaseDatabase.getReference("memos/" + mFirebaseUser.getUid() + "/" + mSelectedMemoKey)
                .setValue(memo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(mEditText, "메모가 수정되었습니다", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void deleteMemo() {

        if(mSelectedMemoKey == null) {
            return;
        }

        Snackbar.make(mEditText, "메모를 삭제하시겠습니까?", Snackbar.LENGTH_LONG).setAction("삭제", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirebaseDatabase
                        .getReference("memos/" + mFirebaseUser.getUid() + "/" + mSelectedMemoKey)
                        .removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                Snackbar.make(mEditText, "삭제가 완료 되었습니다", Snackbar.LENGTH_LONG).show();
                                initMemo();
                            }
                        });
            }
        }).show();

    }
    private void logout() {
        Snackbar.make(mEditText, "로그아웃 하시겠습니까?", Snackbar.LENGTH_LONG).setAction("로그아웃", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirebaseAuth.signOut();
                startActivity(new Intent(MainActivity.this, AuthActivity.class));
                finish();
            }
        }).show();
    }

    private class CheckTypesTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog asyncDialog = new ProgressDialog(
                MainActivity.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("로딩중입니다..");

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                for (int i = 0; i < 5; i++) {
                    //asyncDialog.setProgress(i * 30);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            super.onPostExecute(result);
            Toast.makeText(MainActivity.this, "환영합니다.", Toast.LENGTH_SHORT).show();
        }
    }

}
