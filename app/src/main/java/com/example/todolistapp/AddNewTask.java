package com.example.todolistapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddNewTask extends BottomSheetDialogFragment {
    private TextView setDueDate;
    private EditText mTaskEdit;
    private Button mSave;
    private Spinner dependencySpinner;  // Spinner for selecting task dependency
    private FirebaseFirestore firestore;
    private Context context;
    private String dueDate = "";
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 10;  // Unique request code for voice input
    public static final String TAG = "AddNewTask";

    // List to hold existing tasks (for dependencies)
    private ArrayList<String> taskList = new ArrayList<>();
    private ArrayList<String> taskIds = new ArrayList<>();  // List to store task IDs

    // Create new instance
    public static AddNewTask newInstance() {
        return new AddNewTask();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_new_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDueDate = view.findViewById(R.id.set_due_tv);
        mTaskEdit = view.findViewById(R.id.taskeditText);
        mSave = view.findViewById(R.id.button);
        dependencySpinner = view.findViewById(R.id.dependencies_spinner);

        firestore = FirebaseFirestore.getInstance();
        mSave.setEnabled(false);
        mSave.setBackgroundColor(Color.GRAY);
        ThemeUtils.applyTimeBasedTheme(view, context);

        // Fetch existing tasks for the dependency spinner
        firestore.collection("tasks").whereEqualTo("status", 0).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    taskIds.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String taskName = document.getString("task");
                            String taskId = document.getId();
                            taskList.add(taskName);
                            taskIds.add(taskId);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, taskList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dependencySpinner.setAdapter(adapter);

                        dependencySpinner.setVisibility(View.VISIBLE);  // Show spinner once tasks are loaded
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show());

        // Set up the text watcher to enable/disable the save button based on task input
        mTaskEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    mSave.setEnabled(false);
                    mSave.setBackgroundColor(Color.GRAY);
                } else {
                    mSave.setEnabled(true);
                    mSave.setBackgroundColor(getResources().getColor(R.color.tealblue));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Set up voice input button
        ImageButton btnVoiceInput = view.findViewById(R.id.btnVoiceInput);
        btnVoiceInput.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            try {
                startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, "Your device doesn't support Speech Input", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up due date picker
        setDueDate.setOnClickListener(view1 -> {
            Calendar calendar = Calendar.getInstance();
            int YEAR = calendar.get(Calendar.YEAR);
            int MONTH = calendar.get(Calendar.MONTH);
            int DAY = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(context, (datePicker, year, month, dayOfMonth) -> {
                month = month + 1; // Adjust for 0-based month
                dueDate = dayOfMonth + "/" + month + "/" + year;
                setDueDate.setText(dueDate);
            }, YEAR, MONTH, DAY);

            datePickerDialog.show();
        });

        // Save task to Firestore
        mSave.setOnClickListener(view12 -> {
            String task = mTaskEdit.getText().toString().trim();
            if (task.isEmpty()) {
                Toast.makeText(context, "Empty tasks are not allowed!", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("task", task);
                taskMap.put("due", dueDate);
                taskMap.put("status", 0); // 0 for incomplete
                taskMap.put("time", FieldValue.serverTimestamp());

                // If a dependent task is selected, add its ID as the dependency
                int selectedPosition = dependencySpinner.getSelectedItemPosition();
                if (selectedPosition != AdapterView.INVALID_POSITION) {
                    String dependentTaskId = taskIds.get(selectedPosition);
                    taskMap.put("dependency", dependentTaskId); // Set the dependency task ID
                }

                firestore.collection("tasks").add(taskMap)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(context, "Task Added", Toast.LENGTH_SHORT).show();
                            dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to add task", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Handle the result from voice recognition
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                mTaskEdit.setText(result.get(0)); // Set the recognized speech as the task text
            }
        }
    }
}
