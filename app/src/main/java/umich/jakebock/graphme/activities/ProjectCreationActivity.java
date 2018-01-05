package umich.jakebock.graphme.activities;

import android.Manifest;
import android.app.Activity;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;
import umich.jakebock.graphme.R;
import umich.jakebock.graphme.classes.DataObject;
import umich.jakebock.graphme.classes.DataProject;
import umich.jakebock.graphme.support_classes.DataProjectContainer;

public class ProjectCreationActivity extends AppCompatActivity
{
    private View                 rootView;
    private DataProject          dataProject;
    private DataProjectContainer dataProjectContainer;

    private String projectTitle;
    private String projectImageFilePath = "";

    private int GALLERY_REQUEST_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Call the Super
        super.onCreate(savedInstanceState);

        // Set the Content View
        setContentView(R.layout.activity_project_creation);

        // Initalize the Views
        initializeViews();

        // Initialize the Toolbar
        initializeToolbar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.project_creation_menu, menu);
        return true;
    }

    // Handle the Options Selected in the Fragment
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            // Back Button Clicked
            case android.R.id.home:

                // Return to the Main Activity
                finish();

            // Checkbox Menu Clicked
            case R.id.action_menu_done:

                // Collect the Project Information
                collectDataProjectInformation();

                // Ensure the Project Title is Populated
                if (projectTitle.length() > 0)
                {
                    // Create the New Data Project
                    dataProject = new DataProject(projectTitle, projectImageFilePath);

                    // TODO Create the List of Data Objects

                    // Create the Data Project Container
                    dataProjectContainer = new DataProjectContainer(getApplicationContext());

                    // Create the New Project
                    dataProjectContainer.createProject(dataProject, new ArrayList<DataObject>());

                    // Return to the Main Activity
                    finish();
                }

            default:

                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Detect Request Codes
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            try
            {
                // Fetch the Image Button
                ImageButton imageButton = findViewById(R.id.import_image_image_button);

                // Set the Image Button to Visible
                imageButton.setVisibility(View.VISIBLE);

                // Set the Button to GONE
                findViewById(R.id.import_image_button).setVisibility(View.GONE);

                // Fetch the Data
                Uri selectedImageURI = data.getData();

                // Set the Image to the Bitmap of the Selected Image
                imageButton.setImageBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageURI));

                // Fetch the File for the Image
                projectImageFilePath = getRealPathFromURI(selectedImageURI);
            }

            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }

            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri)
    {
        String[] proj = { MediaStore.Images.Media.DATA };

        //This method was deprecated in API level 11
        //Cursor cursor = managedQuery(contentUri, proj, null, null, null);

        CursorLoader cursorLoader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void collectDataProjectInformation()
    {
        // Fetch the Project Title
        projectTitle = ((EditText) findViewById(R.id.project_name)).getText().toString();
    }

    private void initializeToolbar()
    {
        // Set the Support Action Bar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Fetch the Support Action Toolbar
        ActionBar actionBar = getSupportActionBar();

        // Set the Project Page Title
        actionBar.setTitle(getResources().getString(R.string.title_activity_project_creation));

        // Set the Menu Frame
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);

        // Set the Menu Frame
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void startGalleryRequest()
    {
        String[] galleryPermissions = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
        {
            galleryPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        if ((!galleryPermissions[0].equals("")) && (EasyPermissions.hasPermissions(this, galleryPermissions)))
        {
            startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GALLERY_REQUEST_CODE);;
        }

        else
        {
            EasyPermissions.requestPermissions(this, "Access for storage",101, galleryPermissions);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            startGalleryRequest();
        }
    }

    private void initializeViews()
    {
        // Request Focus on the Edit Text
        findViewById(R.id.project_name).requestFocus();

        // Add Listener for Import Image Button
        findViewById(R.id.import_image_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGalleryRequest();
            }
        });

        // Add Listener for Import Image Button
        // NOTE - This is after the first image has been selected
        findViewById(R.id.import_image_image_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGalleryRequest();
            }
        });
    }
}
