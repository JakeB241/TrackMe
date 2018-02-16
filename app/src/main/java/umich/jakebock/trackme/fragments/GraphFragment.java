package umich.jakebock.trackme.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import umich.jakebock.trackme.R;
import umich.jakebock.trackme.activities.MainActivity;
import umich.jakebock.trackme.classes.DataObject;
import umich.jakebock.trackme.classes.DataProject;
import umich.jakebock.trackme.support_classes.DateTimePicker;


public class GraphFragment extends Fragment
{
    private View                 rootView;
    private DataProject          currentDataProject;
    private GraphView            graphView;
    private ArrayList<DataPoint> dataObjectList;
    private String               currentGraphType;

    private Date startDate;
    private Date endDate;

    public static final List<String> GRAPH_TYPES = Arrays.asList("Line", "Bar", "Point");

    public GraphFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_graph, container, false);

        // Allow the Fragment to Have a Custom Options Menu
        setHasOptionsMenu(true);

        // Fetch the Data Project
        currentDataProject = ((MainActivity) getActivity()).getCurrentDataProject();

        // TODO FIX THIS
        currentGraphType = "Bar";

        if (currentDataProject.getDataObjectList().size() > 0)
        {
            // Initialize the Graph View
            drawGraphView();
        }

        // Return Root View
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // Fetch the Action Bar
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        // Set the Action Bar Title to the Current Data Project Title
        if (actionBar != null) actionBar.setTitle(((MainActivity) getActivity()).getCurrentDataProject().getProjectTitle());

        // Create the Custom Graph Fragment Menu
        inflater.inflate(R.menu.graph_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            // Graph Type Choice
            case R.id.action_menu_graph_choice:
                showGraphTypeSelection();
                break;

            // Date Range Choice
            case R.id.action_menu_date_range:
                showDateRangeSelection();
                break;
        }

        return false;
    }

    private void showGraphTypeSelection()
    {
        // Create the Graph Type Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Graph Type");
        builder.setItems(GRAPH_TYPES.toArray(new String[GRAPH_TYPES.size()]), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int position)
            {
                // Set the Current Graph Type
                currentGraphType = GRAPH_TYPES.get(position);

                // Re Draw the Graph
                drawGraphView();
            }
        });
        builder.show();
    }

    private void showDateRangeSelection()
    {
        // Create the Data Object Prompt View
        final View dataObjectPromptView = LayoutInflater.from(getActivity()).inflate(R.layout.date_range_prompt, null);

        // Create the Alert Dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // Set the Message of the Alert Dialog
        alertDialogBuilder.setMessage("Date Range");

        // Fetch the Data Object Components
        final TextView startDateTextView = dataObjectPromptView.findViewById(R.id.start_date);
        final TextView endDateTextView   = dataObjectPromptView.findViewById(R.id.end_date  );

        // Set the Text of the Data Object
        startDateTextView.setText(currentDataProject.returnDateFormat().format(startDate));
        endDateTextView  .setText(currentDataProject.returnDateFormat().format(endDate  ));

        // Create the Click Listener for the Date Range
        startDateTextView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Show the Date Time Picker
                DateTimePicker dateTimePicker = new DateTimePicker(getActivity(), currentDataProject, view);
                dateTimePicker.showDateTimePicker();
            }
        });

        endDateTextView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Show the Date Time Picker
                DateTimePicker dateTimePicker = new DateTimePicker(getActivity(), currentDataProject, view);
                dateTimePicker.showDateTimePicker();
            }
        });

        // Create the Delete Button
        alertDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                try
                {
                    // Save off the Start Date and End Date
                    startDate = currentDataProject.returnDateFormat().parse(startDateTextView.getText().toString());
                    endDate   = currentDataProject.returnDateFormat().parse(endDateTextView  .getText().toString());

                    // Repopulate the Data Point List
                    drawGraphView();
                }

                catch (ParseException e)
                {
                    e.printStackTrace();
                }
            }
        });

        // Create the Cancel Button
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        alertDialogBuilder.setView(dataObjectPromptView);

        // Show the Alert Dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();

        // Set the Color of the Positive and Negative Button
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    // Compare Class for the Dates
    private class DataObjectInformationCompare implements Comparator<DataPoint>
    {
        @Override
        public int compare(DataPoint dataPoint1, DataPoint dataPoint2)
        {
            return Double.compare(dataPoint1.getY(), dataPoint2.getY());
        }
    }

    // Compare Class for the Objects
    private class DataObjectDateCompare implements Comparator<DataObject>
    {
        @Override
        public int compare(DataObject dataObject1, DataObject dataObject2)
        {
            return dataObject1.getObjectTime().compareTo(dataObject2.getObjectTime());
        }
    }

    private void populateDataPointListInDateRange()
    {
        // Create the Data Points
        dataObjectList = new ArrayList<>();
        for (DataObject dataObject : currentDataProject.getDataObjectList())
        {
            // Fetch the Data of the Data Object
            Date   dataObjectDate        = dataObject.getObjectTime();
            Double dataObjectInformation = Double.parseDouble(dataObject.getObjectInformation());

            // Add the Data Object Information to the List
            if ((dataObjectDate.after(startDate) || dataObjectDate.equals(startDate)) && (dataObjectDate.before(endDate) || dataObjectDate.equals(endDate)))
                dataObjectList.add(new DataPoint(dataObjectDate, dataObjectInformation));
        }
    }

    private void drawGraphView()
    {
        // Fetch the Graph
        graphView = (GraphView) rootView.findViewById(R.id.graph_view);

        // Fetch the Start Date and End Date - Default is the Max/Min
        if (startDate == null && endDate == null)
        {
            startDate = (Collections.min(currentDataProject.getDataObjectList(), new DataObjectDateCompare())).getObjectTime();
            endDate   = (Collections.max(currentDataProject.getDataObjectList(), new DataObjectDateCompare())).getObjectTime();
        }

        // Create the Data Points
        populateDataPointListInDateRange();

        // Set the Number of Horizontal Labels
        graphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(graphView.getContext()));
        //graphView.getGridLabelRenderer().setNumHorizontalLabels(5);
        //graphView.getGridLabelRenderer().setNumVerticalLabels  (20);

        // Fetch the Max/Min Values for the Default Graph
        int minValue = (int) Collections.min(dataObjectList, new DataObjectInformationCompare()).getY();
        int maxValue = (int) Collections.max(dataObjectList, new DataObjectInformationCompare()).getY();

        // Set the X Bounds Manually
        graphView.getViewport().setMinX(startDate.getTime());
        graphView.getViewport().setMaxX(endDate  .getTime());
        graphView.getViewport().setXAxisBoundsManual(true);

        // Set the Y Bounds Manually
        graphView.getViewport().setMinY(Math.floor(minValue));
        graphView.getViewport().setMaxY(Math.ceil (maxValue));
        graphView.getViewport().setYAxisBoundsManual(true);

        // Set the Padding
        graphView.getGridLabelRenderer().setPadding(40);

        // Set Scalable
        graphView.getViewport().setScalable(true);

        // Disable Human Rounding with Dates
        graphView.getGridLabelRenderer().setHumanRounding(false);

        // Create the Graph
        switch (currentGraphType)
        {
            case "Line":
                createLineGraph();
                break;
            case "Bar":
                createBarGraph();
                break;
            case "Point":
                createPointGraph();
                break;
        }
    }

    private void createLineGraph()
    {
        // Remove all Previous Series
        graphView.removeAllSeries();

        // Create the Series
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataObjectList.toArray(new DataPoint[dataObjectList.size()]));
        series.setDrawAsPath(false);
        series.setDrawDataPoints(false);

        series.setOnDataPointTapListener(dataPointTapListener);

        // Add the Series
        graphView.addSeries(series);
    }

    private void createBarGraph()
    {
        // Remove all Previous Series
        graphView.removeAllSeries();

        // Create the Series
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataObjectList.toArray(new DataPoint[dataObjectList.size()]));
        series.setDrawValuesOnTop(true);
        series.setValuesOnTopColor(Color.RED);
        series.setSpacing(15);

        series.setOnDataPointTapListener(dataPointTapListener);

        // Add the Series
        graphView.addSeries(series);
    }

    private void createPointGraph()
    {
        // Remove all Previous Series
        graphView.removeAllSeries();

        // Create the Series
        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>(dataObjectList.toArray(new DataPoint[dataObjectList.size()]));

        series.setOnDataPointTapListener(dataPointTapListener);

        // Add the Series
        graphView.addSeries(series);
    }

    private OnDataPointTapListener dataPointTapListener = new OnDataPointTapListener()
    {
        @Override
        public void onTap(Series series, DataPointInterface dataPoint)
        {
            Toast.makeText(getActivity(), Double.toString(dataPoint.getY()), Toast.LENGTH_SHORT).show();
        }
    };
}