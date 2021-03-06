package umich.jakebock.trackme.support_classes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import umich.jakebock.trackme.R;
import umich.jakebock.trackme.classes.DataObject;
import umich.jakebock.trackme.classes.DataProject;

/**
 * Created by Jake on 1/10/2018.
 */

public class DataObjectListAdapter extends ArrayAdapter<DataObject>
{
    private DataProject dataProject;
    private Context     context;

    // Data Object View Holder
    private static class DataObjectViewHolder
    {
        TextView dataObjectInformationTextView;
        TextView dataObjectDateTime;
    }

    public DataObjectListAdapter(DataProject dataProject, Context context)
    {
        // Call the Super
        super(context, R.layout.data_object_item);

        // Initialize Data
        this.dataProject = dataProject;
        this.context     = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        // Get the data object for this position
        final DataObject dataObject = getItem(position);

        final DataObjectViewHolder dataObjectViewHolder;

        if (convertView == null)
        {
            dataObjectViewHolder = new DataObjectViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.data_object_item, parent, false);
            dataObjectViewHolder.dataObjectInformationTextView = (TextView) convertView.findViewById(R.id.data_object_information_text_view);
            dataObjectViewHolder.dataObjectDateTime            = (TextView) convertView.findViewById(R.id.updated_date);
            convertView.setTag(dataObjectViewHolder);
        }

        else
        {
            dataObjectViewHolder = (DataObjectViewHolder) convertView.getTag();
        }

        // Set the View Parameters
        if (dataObject != null)
        {
            dataObjectViewHolder.dataObjectInformationTextView.setText(dataObject.getObjectInformation());
            dataObjectViewHolder.dataObjectDateTime           .setText(dataProject.returnDateFormat().format(dataObject.getObjectTime()));
        }

        // Return the Completed View
        return convertView;
    }
}
