package com.studymobile.advisos.Services;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.studymobile.advisos.Models.SubjectUser;
import com.studymobile.advisos.Models.UserAvailability;
import com.studymobile.advisos.Models.Week;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class CollectExpertsForChatRoom implements Runnable{
    private final static int NUM_OF_EXPERTS = 5;
    private List<SubjectUser> mExpertUserOfSubjectSelectedId = new ArrayList<>(NUM_OF_EXPERTS);
    private FirebaseDatabase mDatabase;
    private DatabaseReference mSubjectUsersReference;
    private String mSubjectName;
    private UserAvailability mUserAvailability;
    ArrayList<String> mUserIDs = new ArrayList<>();

    public CollectExpertsForChatRoom(String i_subjcetName){
        mSubjectName = i_subjcetName;
        mDatabase = FirebaseDatabase.getInstance();
        mSubjectUsersReference = mDatabase.getReference("SubjectUsers");

    }

    @Override
    public void run() {
        mSubjectUsersReference.child(mSubjectName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                SubjectUser su;
                for(DataSnapshot ds : dataSnapshot.getChildren())
                {
                    su = ds.getValue(SubjectUser.class);

                    if(mExpertUserOfSubjectSelectedId.size() == NUM_OF_EXPERTS &&
                            su.getIsValid() && ds.child("Rating").exists() &&
                            isAvailable(su.getUserId()))
                    {
                        for(SubjectUser x : mExpertUserOfSubjectSelectedId)
                        {
                            if(su.getRating().getAvgRating() > x.getRating().getAvgRating())
                            {
                                mExpertUserOfSubjectSelectedId.remove(x);
                                mExpertUserOfSubjectSelectedId.add(su);
                                break;
                            }
                        }
                    }
                    else if(su.getIsValid() && isAvailable(su.getUserId()))
                    {
                        mExpertUserOfSubjectSelectedId.add(su);
                    }
                }
                notifyAll();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(mExpertUserOfSubjectSelectedId.size() < NUM_OF_EXPERTS)
        {
            //TODO
            // try to collect by location until the list is full
        }

        if(mExpertUserOfSubjectSelectedId.size() < NUM_OF_EXPERTS)
        {   //collect randomly
            getAllUserIds();
            Collections.shuffle(mUserIDs);
            String userID;
            Iterator<String> itr = mUserIDs.iterator();
            while (mExpertUserOfSubjectSelectedId.size() < NUM_OF_EXPERTS && itr.hasNext())
            {
                userID = itr.next();
                if( isAvailable(userID) && !containsUserID(mExpertUserOfSubjectSelectedId, userID) )
                {
                    SubjectUser su = new SubjectUser();
                    su.setUserId(userID);
                    mExpertUserOfSubjectSelectedId.add(su);
                }
            }
        }
    }

    private boolean isAvailable(String i_userID)
    {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(i_userID).child("userAvailability");
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUserAvailability = dataSnapshot.getValue(UserAvailability.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(mUserAvailability.getIsAlwaysAvailable())
            return true;
        if(mUserAvailability.getIsNeverAvailable())
            return false;

        Week weekAvailability = mUserAvailability.getWeekAvailability();

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        Date date = new Date();
        DateFormat format = new SimpleDateFormat("HH:mm");
        String currTime = format.format(date);

        String startTime = null, endTime = null;

        switch (day) {
            case Calendar.SUNDAY:
                // Current day is Sunday
                startTime = weekAvailability.getSunday().getStartTime();
                endTime = weekAvailability.getSunday().getEndTime();
                break;
            case Calendar.MONDAY:
                // Current day is Monday
                startTime = weekAvailability.getMonday().getStartTime();
                endTime = weekAvailability.getMonday().getEndTime();
                break;
            case Calendar.TUESDAY:
                // Current day is Tuesday
                startTime = weekAvailability.getTuesday().getStartTime();
                endTime = weekAvailability.getTuesday().getEndTime();
                break;
            case Calendar.WEDNESDAY:
                // Current day is Wednesday
                startTime = weekAvailability.getWednesday().getStartTime();
                endTime = weekAvailability.getWednesday().getEndTime();
                break;
            case Calendar.THURSDAY:
                // Current day is Thursday
                startTime = weekAvailability.getThursday().getStartTime();
                endTime = weekAvailability.getThursday().getEndTime();
                break;
            case Calendar.FRIDAY:
                // Current day is Friday
                startTime = weekAvailability.getFriday().getStartTime();
                endTime = weekAvailability.getFriday().getEndTime();
                break;
            case Calendar.SATURDAY:
                // Current day is Saturday
                startTime = weekAvailability.getSaturday().getStartTime();
                endTime = weekAvailability.getSaturday().getEndTime();
                break;
        }

        return isBetween(currTime, startTime, endTime);
    }


    private boolean isBetween(String i_currTime, String i_startTime, String i_endTime)
    {
        // all parameters are in format hh:mm

        int currTimehours, currTimeMinutes, startTimeHours, startTimeMinutes, endTimeHours, endTimeMinutes;

        String[] arr = i_currTime.split(":");
        currTimehours = Integer.parseInt(arr[0]);
        currTimeMinutes = Integer.parseInt(arr[1]);

        arr = i_startTime.split(":");
        startTimeHours = Integer.parseInt(arr[0]);
        startTimeMinutes = Integer.parseInt(arr[1]);

        arr = i_endTime.split(":");
        endTimeHours = Integer.parseInt(arr[0]);
        endTimeMinutes = Integer.parseInt(arr[1]);

        if(!(currTimehours >= startTimeHours && currTimehours <= endTimeHours))
            return false;

        if(currTimehours == startTimeHours && currTimeMinutes < startTimeMinutes)
            return false;

        if(currTimehours == endTimeHours && currTimeMinutes > endTimeMinutes)
            return false;

        return true;
    }


    public List<SubjectUser> getmExpertUserOfSubjectSelectedId() {
        return mExpertUserOfSubjectSelectedId;
    }

    private void getAllUserIds()
    {
        FirebaseDatabase.getInstance().getReference("Users").
        addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren())
                {
                    mUserIDs.add(ds.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private boolean containsUserID(List<SubjectUser> i_list, String i_userID)
    {
        for(SubjectUser su : i_list)
        {
            if(su.getUserId().equals(i_userID))
                return true;
        }
        return false;
    }
}
