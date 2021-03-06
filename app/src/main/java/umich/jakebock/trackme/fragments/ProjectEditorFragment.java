package umich.jakebock.trackme.fragments;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

import umich.jakebock.trackme.R;
import umich.jakebock.trackme.activities.MainActivity;
import umich.jakebock.trackme.activities.ProjectCreationActivity;
import umich.jakebock.trackme.classes.DataObject;
import umich.jakebock.trackme.classes.DataProject;
import umich.jakebock.trackme.firebase.FirebaseHandler;
import umich.jakebock.trackme.support_classes.DataProjectListAdapter;

public class ProjectEditorFragment extends Fragment
{
    // region Class Data
    private View                    rootView;
    private FirebaseHandler         firebaseHandler;
    private ListView                projectListView;

    private ArrayList<DataProject>  selectedProjects;
    private ArrayList<View>         selectedViews;
    private DataProjectListAdapter  dataProjectListAdapter;
    // endregion

    // region Constructor
    public ProjectEditorFragment() {}
    // endregion

    // region Lifecycle Functions
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_project_editor, container, false);

        // Populate the Project List
        populateProjectListView();

        // Initialize the Back Stack Listener
        initializeBackStackListener();

        // Initialize Toolbar
        initializeToolbar();

        // Initialize the Add Button
        initializeAddButton();

        // Return the RootView
        return rootView;
    }

    @Override
    public void onResume()
    {
        // Re Populate the Project List
        loadDataProjects();

        // Re Initialize Toolbar
        initializeToolbar();

        // Call the Activity On Resume
        super.onResume();
    }
    //endregion

    // region Initializion Functions
    private void initializeAddButton()
    {
        // Create the Floating Action Button
        FloatingActionButton addButton = (FloatingActionButton) rootView.findViewById(R.id.add_button);

        // Create the Listener for the Add Button
        addButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Show the Project Creation Fragment
                showProjectCreationFragment(null);
            }
        });
    }

    private void initializeToolbar()
    {
        // Fetch the Action Bar
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        // Set the Action Bar Title
        if (actionBar != null) actionBar.setTitle(R.string.toolbar_project_title);
    }

    private void initializeBackStackListener()
    {
        getActivity().getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener()
        {
            public void onBackStackChanged()
            {
                // If the Back Stack is Empty, We arrived here from the ProjectBreakdownFragment
                // Repopulate the Project List (For the Entries)
                if (getActivity().getSupportFragmentManager().getBackStackEntryCount() == 0)
                    loadDataProjects();

                // Restore the Toolbar
                ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
                if (actionBar != null) actionBar.setTitle(R.string.toolbar_project_title);

                getActivity().invalidateOptionsMenu();
            }
        });
    }
    // endregions

    // region Fragment Specific Functions
    private void populateProjectListView()
    {
        // Fetch the List View
        projectListView = rootView.findViewById(R.id.project_list_view);

        // Create the List Adapter
        dataProjectListAdapter = new DataProjectListAdapter(getActivity().getApplicationContext());

        // Set the Adapter for the List View
        projectListView.setAdapter(dataProjectListAdapter);

        // Set the Action Mode Callback
        projectListView.setMultiChoiceModeListener(new DataProjectActionModeCallback());

        // Set the On Click for the Project List View
        projectListView.setOnItemClickListener(dataProjectItemClickedListener);
    }

    private void loadDataProjects()
    {
        // Create the FireBase Handler
        firebaseHandler = new FirebaseHandler(getActivity());

        // Set the Listener for the FireBase Handler
        firebaseHandler.setListener(dataLoadCompletedListener);

        // Clear Previous
        dataProjectListAdapter.clear();

        // Load All Projects from Device Memory (Async Call to Load the Projects)
        firebaseHandler.loadProjects();
    }

    private void showDeleteAlertDialog()
    {
        // Create the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the Message that will be Displayed
        String message = "";
        if (selectedProjects.size() == 1) message = (selectedProjects.get(0).getProjectTitle());
        else                              message =  selectedProjects.size() + " Projects";

        // Set the Message of the Alert Dialog
        builder.setMessage(message + " will be deleted.");

        // Create the Delete Button
        builder.setPositiveButton("Delete", deleteButtonListener);

        // Create the Cancel Button
        builder.setNegativeButton("Cancel", cancelButtonListener);

        // Show the Alert Dialog
        AlertDialog alert = builder.create();
        alert.show();

        // Set the Color of the Positive and Negative Button
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    private void shareProject(DataProject dataProject)
    {
        // Create the Share Intent
        Intent sendIntent         = new Intent(Intent.ACTION_SEND);
        String projectTitle       = dataProject.getProjectTitle();
        String projectInformation = "";

        // Store the Data Object List
        ArrayList<DataObject> dataObjects = dataProject.getDataObjectList();

        // Sort the Data
        Collections.sort(dataObjects, DataObject.sortAscendingOrder);

        // Create the Data String
        for (DataObject data : dataObjects)
        {
            projectInformation += data.getObjectInformation() + " " + dataProject.returnDateFormat().format(data.getObjectTime()) + "\n";
        }

        // Add the Title
        projectInformation = projectTitle + "\n" + projectInformation;

        sendIntent.putExtra(Intent.EXTRA_TEXT, projectInformation);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share)));
    }

    // endregion

    // Listener for the Data Object List Adapter
    FirebaseHandler.DataLoadCompletedListener dataLoadCompletedListener = new FirebaseHandler.DataLoadCompletedListener()
    {
        @Override
        public void dataProjectLoadCompleted(ArrayList<DataProject> loadedDataProjects)
        {
            // Add all of the Projects
            dataProjectListAdapter.addAll(loadedDataProjects);

            // Notify the Data Set Changed
            dataProjectListAdapter.notifyDataSetChanged();
        }

        @Override
        public void dataProjectsDeletedCompleted(ArrayList<DataProject> deletedDataProjects)
        {
            // Remove all of the Projects from the List
            for (DataProject dataProject : deletedDataProjects)
                dataProjectListAdapter.remove(dataProject);

            // Notify the Data Set Changed
            dataProjectListAdapter.notifyDataSetChanged();
        }

        @Override
        public void dataProjectExistsCompleted(Boolean projectExists) {}

        @Override
        public void dataProjectCreatedCompleted() {}
    };

    // region Click Listeners
    private AdapterView.OnItemClickListener dataProjectItemClickedListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            // Set the Current Data Project
            ((MainActivity)getActivity()).setCurrentProject((DataProject) projectListView.getItemAtPosition(position));

            // Transition to the Project Breakdown Fragment
            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).
                    add(R.id.content_main, new ProjectBreakdownFragment()).
                    addToBackStack("ProjectEditorFragment").
                    commit();
        }
    };

    private DialogInterface.OnClickListener deleteButtonListener = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            // Create the Slide out Right Animation
            Animation anim = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), android.R.anim.slide_out_right);

            // Loop through the Selected Views and Start the Animation
            for (View view : selectedViews) view.startAnimation(anim);

            // Set the Animation Listener
            anim.setAnimationListener(new DeleteDataProjectAnimationListener());
        }
    };

    private DialogInterface.OnClickListener cancelButtonListener = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            dialog.dismiss();
        }
    };

    //endregion

    // region Listener Classes
    // Listener for Animation Listener Deleting the Data Project (Slide Right)
    private class DeleteDataProjectAnimationListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {
            // Delete the Projects (Async Call to Delete the Projects)
            firebaseHandler.deleteProjects(selectedProjects);
        }

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
    }

    // Listener for the Action Mode Callback for the Action Bar (Long Click on List Items)
    private class DataProjectActionModeCallback implements ListView.MultiChoiceModeListener
    {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
        {
            // Fetch the Data Project of the Selected Item
            DataProject selectedDataProject = (DataProject) projectListView.getItemAtPosition(position);
            View selectedView               = (View)        projectListView.getChildAt(position);

            // Add/Remove from the Selected Projects List
            if (checked)
            {
                selectedProjects.add(selectedDataProject);
                selectedViews   .add(selectedView);
            }

            else
            {
                selectedProjects.remove(selectedDataProject);
                selectedViews   .remove(selectedView);
            }

            // Remove the Edit and Share Button there are More Than One Selected Projects
            if (selectedProjects.size() > 1)
            {
                mode.getMenu().findItem(R.id.action_menu_edit).setVisible(false);
                mode.getMenu().findItem(R.id.action_menu_share).setVisible(false);
            }

            else
            {
                mode.getMenu().findItem(R.id.action_menu_edit).setVisible(true);
                mode.getMenu().findItem(R.id.action_menu_share).setVisible(true);
            }
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            // Initialize the Selected Projects
            selectedProjects = new ArrayList<>();

            // Initialize the Selected Views
            selectedViews = new ArrayList<>();

            // Inflate the Project Editor Edit Menu
            getActivity().getMenuInflater().inflate(R.menu.data_project_action_mode_menu, menu);

            // Set the Title
            mode.setTitle(R.string.edit_projects_title);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            // Switch for the Item ID
            switch (item.getItemId())
            {
                case R.id.action_menu_edit:
                    showProjectCreationFragment(selectedProjects.get(0));
                    break;
                case R.id.action_menu_delete:
                    showDeleteAlertDialog();
                    break;
                case R.id.action_menu_share:
                    shareProject(selectedProjects.get(0));
                    break;
                default:
                    return false;
            }

            // Finish the Action Mode
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {}
    }
    //endregion

    // region Navigation Functions
    private void showProjectCreationFragment(DataProject dataProject)
    {
        // Create the Project Creation Activity
        Intent projectCreationIntent = new Intent(getActivity(), ProjectCreationActivity.class);

        // Add Extra to the Intent
        if (dataProject != null) projectCreationIntent.putExtra("DATA_PROJECT", dataProject);

        // Create the Project Creation Activity with Animation
        startActivity(projectCreationIntent);
        getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
    // endregion
}
