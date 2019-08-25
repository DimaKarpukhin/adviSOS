package com.studymobile.advisos.Models;

public class SubjectUser
{
    private String mUserId;
    private Rating mRating;

    public SubjectUser(){}

    public SubjectUser(String i_UserId, Rating i_Rating)
    {
        this.mUserId = mUserId;
        this.mRating = mRating;
    }

    public String GetUserId()
    {
        return mUserId;
    }

    public Rating GetRating()
    {
        return mRating;
    }

    public void SetUserId(String i_UserId)
    {
        this.mUserId = mUserId;
    }

    public void SetRating(Rating i_Rating)
    {
        this.mRating = i_Rating;
    }
}
