package umich.jakebock.trackme.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import umich.jakebock.trackme.R;
import umich.jakebock.trackme.activities.MainActivity;
import umich.jakebock.trackme.classes.DataObject;
import umich.jakebock.trackme.classes.DataProject;
import umich.jakebock.trackme.firebase.FirebaseHandler;
import umich.jakebock.trackme.support_classes.DataObjectListAdapter;

public class ListFragment extends Fragment
{
    private View                   rootView;
    private DataProject            currentDataProject;
    private DataObjectListAdapter  dataObjectListAdapter;
    private ListView               dataObjectListView;
    private FirebaseHandler        firebaseHandler;

    private ArrayList<DataObject>  selectedDataObjects;
    private ArrayList<View>        selectedViews;

    public ListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Create the Root View
        rootView = inflater.inflate(R.layout.fragment_list, container, false);

        // Fetch the Data Project
        currentDataProject = ((MainActivity)getActivity()).getCurrentDataProject();

        // Initialize the Data Object List View
        initializeDataObjectListView();

        // Initialize the Floating Action Button
        initializeAddDataObjectButton();

        // Allow the Fragment to Have a Custom Options Menu
        setHasOptionsMenu(true);

        // Create the FireBase Handler
        firebaseHandler = new FirebaseHandler(getActivity());

        // Set the Listener for the FireBase Handler
        firebaseHandler.setListener(dataLoadCompletedListener);

        // Return the Root View
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // Fetch the Action Bar
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        // Set the Action Bar Title to the Current Data Project Title
        if (actionBar != null) actionBar.setTitle(((MainActivity) getActivity()).getCurrentDataProject().getProjectTitle());
    }

    @Override
    public void onDestroyView()
    {
        // Collect the Data Objects
        //collectAndSaveDataObjects();

        // Call the Super
        super.onDestroyView();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        // Call the Super
        super.setUserVisibleHint(isVisibleToUser);

        // Collect the Data when the View Has been Created and the
        // Fragment is no longer visible to the User
        if (rootView != null && !isVisibleToUser)
        {
            // Collect the Data Objects and Set the Current Data Project
            //collectAndSaveDataObjects();
        }
    }

    private void initializeDataObjectListView()
    {
        // Fetch the List View
        dataObjectListView = (ListView) rootView.findViewById(R.id.data_object_list_view);

        // Initialize the List Adapter
        dataObjectListAdapter = new DataObjectListAdapter(getActivity().getApplicationContext());

        // Set the Data Objects for the List Adapter
        dataObjectListAdapter.addAll(currentDataProject.getDataObjectList());

        // Sort the Data Objects
        notifyDataSetChangedAndSort();

        // Set the Adapter for the List View
        dataObjectListView.setAdapter(dataObjectListAdapter);

        // Set the Action Mode Callback
        dataObjectListView.setMultiChoiceModeListener(new DataObjectActionModeCallback());

        // Set the On Click for the Project List View
        dataObjectListView.setOnItemClickListener(dataObjectItemClickedListener);
    }

    private void initializeAddDataObjectButton()
    {
        // Fetch the Floating Action Button
        FloatingActionButton addButton = rootView.findViewById(R.id.add_button);

        // Set the Add Button
        addButton.setImageResource(android.R.drawable.ic_input_add);

        // Create the Listener for the Add Button
        addButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                // Show the Data Object Creation Prompt
                showDataObjectCreationPrompt(null);
            }
        });
    }

    private void showDateAndTimePicker(final View chosenView)
    {
        // Parse the Current Time from the View
        final TextView updateTimeLabel = (TextView) chosenView;
        final Calendar calendar        = new GregorianCalendar();

        try
        {
            // Parse the Displayed Date and Set the Time
            Date displayedDate = MainActivity.dateFormat.parse(updateTimeLabel.getText().toString());
            calendar.setTime(displayedDate);
        }

        catch (ParseException e)
        {
            e.printStackTrace();
        }

        // Launch the Date Picker
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), R.style.DialogTheme, new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                // Fetch the Chosen Dates
                final String chosenDate = (monthOfYear + 1) + " " + (dayOfMonth) + " " + (year);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), R.style.DialogTheme, new TimePickerDialog.OnTimeSetListener()
                {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute)
                    {
                        // Fetch the Chosen Time
                        final String chosenTime = hourOfDay + " " + minute;

                        try
                        {
                            // Create the Date Parser
                            SimpleDateFormat dateParser = new SimpleDateFormat("MM dd yyyy HH mm", Locale.US);

                            // Populate the Text View
                            updateTimeLabel.setText(MainActivity.dateFormat.format(dateParser.parse(chosenDate + " " + chosenTime)));
                        }
                        catch (ParseException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);

                timePickerDialog.show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Show the Data Picker
        datePickerDialog.show();
    }

    private void showDeleteAlertDialog()
    {
        // Create the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set the Message of the Alert Dialog
        builder.setMessage(selectedDataObjects.size() + " Data Object(s) will be deleted.");

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
            anim.setAnimationListener(new DeleteDataObjectAnimationListener());
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

    private class DeleteDataObjectAnimationListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation)
        {
            // Clear the Adapter
            dataObjectListAdapter.clear();

            // Fetch all of the Current Data Objects
            ArrayList<DataObject> dataObjects = currentDataProject.getDataObjectList();

            // Remove the Deleted Data Objects from the Data Object list
            dataObjects.removeAll(selectedDataObjects);

            // Set the Data Object List
            currentDataProject.setDataObjectList(dataObjects);

            // Set the Current Project
            ((MainActivity) getActivity()).setCurrentProject(currentDataProject);

            // Delete the Projects
            dataObjectListAdapter.addAll(currentDataProject.getDataObjectList());

            // Overwrite the Project with the New Data
            firebaseHandler.createProject(currentDataProject);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}
    }

    private void showDataObjectCreationPrompt(final DataObject dataObject)
    {
        // Create the Data Object Prompt View
        final View dataObjectPromptView = LayoutInflater.from(getActivity()).inflate(R.layout.data_object_prompt, null);

        // Create the Alert Dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // Fetch the Data Object Components
        final EditText dataObjectInformation = dataObjectPromptView.findViewById(R.id.data_object_information);
        final TextView dataObjectTime        = dataObjectPromptView.findViewById(R.id.data_object_time       );

        if (dataObject != null)
        {
            // Set the Text of the Data Object
            dataObjectInformation.setText(dataObject.getObjectInformation());
            dataObjectTime       .setText(dataObject.returnDateString());
        }

        else
        {
            // Set the Current Date
            dataObjectTime.setText(MainActivity.dateFormat.format(new Date()));
        }

        // Create the Listener for the Data Object Time
        dataObjectTime.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showDateAndTimePicker(view);
            }
        });

        // Set the View of the Alert Dialog
        alertDialogBuilder.setView(dataObjectPromptView);

        // Set Alert Dialog Information
        alertDialogBuilder.setCancelable(false).setPositiveButton("Save", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (dataObject != null)
                {
                    // Edit a Current Data Object
                    dataObject.setObjectInformation(dataObjectInformation.getText().toString());
                    dataObject.setDateAsString     (dataObjectTime       .getText().toString());
                }

                // Create a New Data Object
                else
                {
                    // Create a New Data Object
                    dataObjectListAdapter.add(new DataObject(dataObjectInformation.getText().toString(), dataObjectTime.getText().toString()));
                }

                // Fetch all of the Data Objects
                ArrayList<DataObject> dataObjects = new ArrayList<>();
                for (int i = 0; i < dataObjectListAdapter.getCount(); i++)
                    dataObjects.add(dataObjectListAdapter.getItem(i));

                // Set the Data Object List
                currentDataProject.setDataObjectList(dataObjects);

                // Set the Current Project
                ((MainActivity) getActivity()).setCurrentProject(currentDataProject);

                // Update the Current Project
                firebaseHandler.createProject(currentDataProject);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id)
            {
                dialog.cancel();
            }
        });

        // Create the Alert Dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // Show the Alert Dialog
        alertDialog.show();

        // Set the Color of the Positive and Negative Button
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    private AdapterView.OnItemClickListener dataObjectItemClickedListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            // Set the Current Data Project
            final DataObject dataObject = (DataObject) dataObjectListView.getItemAtPosition(position);

            // Create the Data Object Prompt View
            showDataObjectCreationPrompt(dataObject);
        }
    };

    // Listener for the Data Object List Adapter
    FirebaseHandler.DataLoadCompletedListener dataLoadCompletedListener = new FirebaseHandler.DataLoadCompletedListener()
    {
        @Override
        public void dataProjectExistsCompleted(Boolean projectExists) {}

        @Override
        public void dataProjectCreatedCompleted()
        {
            // Sort and Notify the Data Set Changed
            notifyDataSetChangedAndSort();
        }

        @Override
        public void dataProjectsDeletedCompleted(ArrayList<DataProject> deletedDataProjects) {}

        @Override
        public void dataProjectLoadCompleted(ArrayList<DataProject> loadedDataProjects) {}

    };

    private void notifyDataSetChangedAndSort()
    {
        // Sort the Adapter Data
        dataObjectListAdapter.sort(new Comparator<DataObject>()
        {
            @Override
            public int compare(DataObject dataObject1, DataObject dataObject2)
            {
                return dataObject2.getObjectTime().compareTo(dataObject1.getObjectTime());
            }
        });

        // Notify the Data Set Changed
        dataObjectListAdapter.notifyDataSetChanged();
    }


    // Listener for the Action Mode Callback for the Action Bar (Long Click on List Items)
    private class DataObjectActionModeCallback implements ListView.MultiChoiceModeListener
    {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
        {
            // Fetch the Data Project of the Selected Item
            DataObject selectedDataObject   = (DataObject) dataObjectListView.getItemAtPosition(position);
            View       selectedView         = (View)       dataObjectListView.getChildAt(position);

            // Add/Remove from the Selected Projects List
            if (checked)
            {
                selectedDataObjects.add(selectedDataObject);
                selectedViews      .add(selectedView);
            }

            else
            {
                selectedDataObjects.remove(selectedDataObject);
                selectedViews      .remove(selectedView);
            }
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            // Initialize the Selected Data Objects
            selectedDataObjects = new ArrayList<>();

            // Initialize the Selected Views
            selectedViews       = new ArrayList<>();

            // Inflate the Project Editor Edit Menu
            getActivity().getMenuInflater().inflate(R.menu.data_object_action_mode_menu, menu);

            // Set the Title
            mode.setTitle(R.string.edit_objects_title);

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
                case R.id.action_menu_delete:
                    showDeleteAlertDialog();
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
}