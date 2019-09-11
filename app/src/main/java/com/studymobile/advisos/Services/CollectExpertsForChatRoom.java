package com.studymobile.advisos.Services;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.studymobile.advisos.Models.Day;
import com.studymobile.advisos.Models.Rating;
import com.studymobile.advisos.Models.SubjectUser;
import com.studymobile.advisos.Models.User;
import com.studymobile.advisos.Models.UserAvailability;
import com.studymobile.advisos.Models.UserLocation;
import com.studymobile.advisos.Models.Week;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CollectExpertsForChatRoom
{
    private final static int NUM_OF_EXPERTS = 5;
    private List<SubjectUser> mExpertUserOfSubjectSelectedId = new ArrayList<>(NUM_OF_EXPERTS);
    private FirebaseDatabase mDatabase;
    private DatabaseReference  mUsersReference;
    private DatabaseReference mSubjectUsersReference;
    private String mSubjectName;
    private UserAvailability mUserAvailability;
    private ArrayList<String> mUserIDs = new ArrayList<>();
    private UserLocation mOpenerLoc = null;



    ///=========================>>>>>>>>>>
    private List<String> mExperts = new ArrayList<>();
    private List<User> mAvailableUsers = new ArrayList<>();
    private List<PairUserIdAndAvgRating> mPairsUserIdAndAvgRating = new ArrayList<>();
    private List<PairUserIdAndDistance> mPairsUserIdAndDistance = new ArrayList<>();
    ///<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


    private class PairUserIdAndAvgRating implements Comparable
    {
        private String mUserID;
        private float mAvgRating;

        public String getUserID() {
            return mUserID;
        }

        public void setUserID(String i_userID) {
            mUserID = i_userID;
        }

        public float getAvgRating() {
            return mAvgRating;
        }

        public void setAvgRating(float i_avgRating) {
            mAvgRating = i_avgRating;
        }

        @Override
        public int compareTo(Object o) {
            PairUserIdAndAvgRating pair = (PairUserIdAndAvgRating)o;
            if(mAvgRating < pair.getAvgRating())
                return (-1);
            if(mAvgRating > pair.getAvgRating())
                return 1;
            return 0;
        }
    }


    private class PairUserIdAndDistance implements Comparable
    {
        private String mUserID;
        private double mDistance;

        public String getUserID() {
            return mUserID;
        }

        public void setUserID(String i_userID) {
            this.mUserID = i_userID;
        }

        public double getDistance() {
            return mDistance;
        }

        public void setDistance(double i_distance) {
            this.mDistance = i_distance;
        }

        @Override
        public int compareTo(Object o) {
            PairUserIdAndDistance pair = (PairUserIdAndDistance)o;
            if(mDistance < pair.mDistance)
                return (-1);
            if(mDistance > pair.mDistance)
                return 1;
            return 0;
        }
    }


    public CollectExpertsForChatRoom(String i_subjcetName){
        mSubjectName = i_subjcetName;
        mDatabase = FirebaseDatabase.getInstance();
        mSubjectUsersReference = mDatabase.getReference("SubjectUsers");
        mUsersReference = mDatabase.getReference("Users");

    }


    public void run() {
        mUsersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                getAllAvailableUsers(dataSnapshot);
                mSubjectUsersReference.child(mSubjectName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        getByRating(dataSnapshot);
                        Collections.sort(mPairsUserIdAndAvgRating);
                        Collections.reverse(mPairsUserIdAndAvgRating);
                        /* mPairsUserIdAndAvgRating is ready to iterate over */
                        for(PairUserIdAndAvgRating pair : mPairsUserIdAndAvgRating)
                        {
                            if(mExperts.size() == NUM_OF_EXPERTS)
                                break;
                            mExperts.add(pair.getUserID());
                        }

                        if(mExperts.size() == NUM_OF_EXPERTS)
                        {
                            return;
                        }

                        getByLocation();
                        Collections.sort(mPairsUserIdAndDistance);
                        /* mPairsUserIdAndDistance is ready to iterate over */
                        for(PairUserIdAndDistance pair : mPairsUserIdAndDistance)
                        {
                            if(mExperts.size() == NUM_OF_EXPERTS)
                                break;
                            mExperts.add(pair.getUserID());
                        }

                        if(mExperts.size() == NUM_OF_EXPERTS)
                        {
                            return;
                        }

                        /* adding users randomly*/
                        Collections.shuffle(mAvailableUsers);
                        for(User user : mAvailableUsers)
                        {
                            if(mExperts.size() == NUM_OF_EXPERTS)
                                break;
                            mExperts.add(user.getUserId());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
//                SubjectUser su;
//                for(DataSnapshot ds : dataSnapshot.getChildren())
//                {
//                    su = ds.getValue(SubjectUser.class);
//
//                    if(mExpertUserOfSubjectSelectedId.size() == NUM_OF_EXPERTS &&
//                            su.getIsValid() && ds.child("Rating").exists() &&
//                            isAvailable(su.getUserId()))
//                    {
//                        for(SubjectUser x : mExpertUserOfSubjectSelectedId)
//                        {
//                            if(su.getRating().getAvgRating() > x.getRating().getAvgRating())
//                            {
//                                mExpertUserOfSubjectSelectedId.remove(x);
//                                mExpertUserOfSubjectSelectedId.add(su);
//                                break;
//                            }
//                        }
//                    }
//                    else if(su.getIsValid() && isAvailable(su.getUserId()))
//                    {
//                        mExpertUserOfSubjectSelectedId.add(su);
//                    }
//                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private UserAvailability retrieveUserAvailabilityFromDB(DataSnapshot i_DataSnapshot)
    {
        UserAvailability userAvailability;
        userAvailability = i_DataSnapshot.getValue(UserAvailability.class);
        if (i_DataSnapshot.child("userAvailability").exists())
        {
            String path = "userAvailability/weekAvailability/";
            if (i_DataSnapshot.child(path).exists())
            {
                Week week = i_DataSnapshot.getValue(Week.class);
                if (i_DataSnapshot.child(path + "sunday").exists()) {
                    week.setSunday(i_DataSnapshot.child(path + "sunday").getValue(Day.class));
                }
                if (i_DataSnapshot.child(path + "monday").exists()) {
                    week.setMonday(i_DataSnapshot.child(path + "monday").getValue(Day.class));
                }
                if (i_DataSnapshot.child(path + "tuesday").exists()) {
                    week.setTuesday(i_DataSnapshot.child(path + "tuesday").getValue(Day.class));
                }
                if (i_DataSnapshot.child(path + "wednesday").exists()) {
                    week.setWednesday(i_DataSnapshot.child(path + "wednesday").getValue(Day.class));
                }
                if (i_DataSnapshot.child(path + "thursday").exists()) {
                    week.setThursday(i_DataSnapshot.child(path + "thursday").getValue(Day.class));
                }
                if (i_DataSnapshot.child(path + "friday").exists()) {
                    week.setFriday(i_DataSnapshot.child(path + "friday").getValue(Day.class));
                }
                if (i_DataSnapshot.child(path + "saturday").exists()) {
                    week.setSaturday(i_DataSnapshot.child(path + "saturday").getValue(Day.class));
                }

                userAvailability.setWeekAvailability(week);
            }
        }
        return userAvailability;
    }


    private void getAllAvailableUsers(DataSnapshot dataSnapshot)
    {
        UserAvailability userAvailability;
        for(DataSnapshot ds : dataSnapshot.getChildren())
        {
            if(ds.child("userAvailability").exists())
            {
                userAvailability = retrieveUserAvailabilityFromDB(ds.child("userAvailability"));
                if(userAvailability.getIsNeverAvailable())
                {
                    continue;
                }

                if(userAvailability.getIsNotDisturb())
                {
                    continue;
                }

                User user = ds.getValue(User.class);
                UserLocation userLocation;
                if(ds.child("userLocation").exists())
                {
                    userLocation = ds.child("userLocation").getValue(UserLocation.class);
                }
                else
                {
                    userLocation = null;
                }

                user.setUserLocation(userLocation);
                user.setUserId(ds.getKey());

                if(userAvailability.getIsAlwaysAvailable())
                {
                    mAvailableUsers.add(user);
                    continue;
                }

                Week weekAvailability  = userAvailability.getWeekAvailability();
                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_WEEK);

                Date date = new Date();
                DateFormat format = new SimpleDateFormat("HH:mm");
                String currTime = format.format(date);

                String startTime = null, endTime = null;

                switch (day) {
                    case Calendar.SUNDAY:
                        // Current day is Sunday
                        if(weekAvailability.getSunday() == null)
                            return;
                        startTime = weekAvailability.getSunday().getStartTime();
                        endTime = weekAvailability.getSunday().getEndTime();
                        break;
                    case Calendar.MONDAY:
                        // Current day is Monday
                        if(weekAvailability.getMonday() == null)
                            return;
                        startTime = weekAvailability.getMonday().getStartTime();
                        endTime = weekAvailability.getMonday().getEndTime();
                        break;
                    case Calendar.TUESDAY:
                        // Current day is Tuesday
                        if(weekAvailability.getTuesday() == null)
                            return;
                        startTime = weekAvailability.getTuesday().getStartTime();
                        endTime = weekAvailability.getTuesday().getEndTime();
                        break;
                    case Calendar.WEDNESDAY:
                        // Current day is Wednesday
                        if(weekAvailability.getWednesday() == null)
                            return;
                        startTime = weekAvailability.getWednesday().getStartTime();
                        endTime = weekAvailability.getWednesday().getEndTime();
                        break;
                    case Calendar.THURSDAY:
                        // Current day is Thursday
                        if(weekAvailability.getThursday() == null)
                            return;
                        startTime = weekAvailability.getThursday().getStartTime();
                        endTime = weekAvailability.getThursday().getEndTime();
                        break;
                    case Calendar.FRIDAY:
                        // Current day is Friday
                        if(weekAvailability.getFriday() == null)
                            return;
                        startTime = weekAvailability.getFriday().getStartTime();
                        endTime = weekAvailability.getFriday().getEndTime();
                        break;
                    case Calendar.SATURDAY:
                        // Current day is Saturday
                        if(weekAvailability.getSaturday() == null)
                            return;
                        startTime = weekAvailability.getSaturday().getStartTime();
                        endTime = weekAvailability.getSaturday().getEndTime();
                        break;
                }

                if(isBetween(currTime, startTime, endTime))
                {
                    mAvailableUsers.add(user);
                }
            }
        }
    }


    private void getByRating(DataSnapshot dataSnapshot)
    {
        SubjectUser subjectUser;
        for(DataSnapshot ds : dataSnapshot.getChildren())
        {
           subjectUser = ds.getValue(SubjectUser.class);

           /*---------------------------------------------------*/
            //should continue if userId not in mAvailableUsers
            boolean found = false;
           for(User user : mAvailableUsers)
           {
               if(user.getUserId().equals(subjectUser.getUserId()))
               {
                   found = true;
                   break;
               }
           }
           if(!found)
           {
               continue;
           }
            /*---------------------------------------------------*/
           if( !subjectUser.getIsValid())
           {
               continue;
           }

            PairUserIdAndAvgRating pair = new PairUserIdAndAvgRating();
            pair.setUserID(subjectUser.getUserId());

           if(ds.child("rating").exists())
           {
                subjectUser.setRating(ds.child("rating").getValue(Rating.class));
                pair.setAvgRating(subjectUser.getRating().getAvgRating());
           }
           else
           {
               pair.setAvgRating(0);
           }

            mPairsUserIdAndAvgRating.add(pair);
        }
    }


    private void getByLocation()
    {
        PairUserIdAndDistance pair = new PairUserIdAndDistance();
        for(User user : mAvailableUsers)
        {
            if(user.getLocation() != null)
            {
                pair.setUserID(user.getUserId());
                pair.setDistance(mOpenerLoc.distanceBetween(user.getLocation()));
                mPairsUserIdAndDistance.add(pair);
            }
        }
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


    public List<SubjectUser> getExpertUserOfSubjectSelectedId() {
        return mExpertUserOfSubjectSelectedId;
    }
}