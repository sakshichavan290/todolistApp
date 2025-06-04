package com.example.todolistapp.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolistapp.AddNewTask;
import com.example.todolistapp.MainActivity;
import com.example.todolistapp.Model.TaskId;
import com.example.todolistapp.Model.ToDoModel;
import com.example.todolistapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.MyViewHolder> {
    private List<ToDoModel> todoList;
    private MainActivity activity;
    private FirebaseFirestore firestore;

    public ToDoAdapter(MainActivity mainActivity , List<ToDoModel>  todoList){
        this.todoList = todoList;
        activity = mainActivity;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View view = LayoutInflater.from(activity).inflate(R.layout.each_task , parent , false);
       firestore=FirebaseFirestore.getInstance();
       return new MyViewHolder(view);
    }

    public void deleteTask(int position){
        ToDoModel toDoModel = todoList.get(position);
        firestore.collection("task").document(toDoModel.TaskId).delete();
        todoList.remove(position);
        notifyItemRemoved(position);
    }

    public Context getContext(){
        return activity;
    }


    public void editTask(int position){
        ToDoModel toDoModel= todoList.get(position);
        Bundle bundle= new Bundle();
        bundle.putString("task",toDoModel.getTask());
        bundle.putString("due",toDoModel.getDue());
        bundle.putString("id",toDoModel.TaskId);
        AddNewTask addNewTask = new AddNewTask();
        addNewTask.setArguments(bundle);
        addNewTask.show(activity.getSupportFragmentManager() , addNewTask.getTag());

    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ToDoModel toDoModel=todoList.get(position);
        holder.mcheckBox.setText(toDoModel.getTask());
        holder.mDueDateTv.setText("Due on"+ toDoModel.getDue());

        holder.mcheckBox.setChecked(toBoolean(toDoModel.getStatus()));
        holder.mcheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    firestore.collection( "tasks").document(toDoModel.TaskId).update("Status", 1);

                }
                else {
                    firestore.collection("tasks").document(toDoModel.TaskId).update("status" , 0);
                }

            }
        });


    }
    private boolean toBoolean(int status){
        return status !=0;
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView mDueDateTv;
        CheckBox mcheckBox;


         public MyViewHolder(@NonNull View itemView) {
             super(itemView);
             mDueDateTv = itemView.findViewById(R.id.due_date_tv);
             mcheckBox=itemView.findViewById(R.id.mcheckbox);
         }
     }
}
