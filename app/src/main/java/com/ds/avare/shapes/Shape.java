/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.shapes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.ds.avare.place.Plan;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Scale;
import com.ds.avare.utils.Helper;
import com.sromku.polygon.Point;
import com.sromku.polygon.Polygon;
import com.sromku.polygon.Polygon.Builder;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

/**
 * @author zkhan
 * @author plinel
 *
 */
public abstract class Shape {

    protected ArrayList<Coordinate> mCoords;
    protected double mLonMin;
    protected double mLonMax;
    protected double mLatMin;
    protected double mLatMax;
    
    protected String mText;
    
    private Builder mPolyBuilder;
    private Polygon mPoly;

    private Date mDate;
    
    /**
     * 
     */
    public Shape(String label, Date date) {
        mCoords = new ArrayList<Coordinate>();
        mLonMin = 180;
        mLonMax = -180;
        mLatMin = 180;
        mLatMax = -180;
        mText = label;
        mDate = date;
        mPolyBuilder = Polygon.Builder(); 
    }

    public Date getDate() {
        return mDate;
    }

    /**
     *
     * @return
     */
    public boolean isOld(int expiry) {
        if(mDate == null) {
            return false;
        }
        long diff = Helper.getMillisGMT();
        diff -= mDate.getTime();

        return diff > (expiry * 60 * 1000);
    }

    /**
     * 
     * @param lon
     * @param lat
     * @param issep
     */
    public void add(double lon, double lat, boolean issep) {
    	add(lon,lat,issep, 0);
    }

    /**
     *
     * @param lon
     * @param lat
     * @param issep
     * @param segment
     */
    public void add(double lon, double lat, boolean issep, int segment) {
        Coordinate c = new Coordinate(lon, lat);
        if(issep) {
            c.makeSeparate();
        }
        c.setSegment(segment);
        
        mCoords.add(c);
        mPolyBuilder.addVertex(new Point((float)lon, (float)lat));
        
        /*
         * Calculate start points
         */
        if(lon < mLonMin) {
             mLonMin = lon;
        }
        if(lon >= mLonMax) {
             mLonMax = lon;
        }
        if(lat < mLatMin) {
             mLatMin = lat;
        }
        if(lat >= mLatMax) {
             mLatMax = lat;
        }
    }

    public void drawShape(Canvas c, Origin origin, Scale scale, Movement movement, Paint paint, boolean night, boolean drawTrack) {
    	drawShape(c, origin, scale,movement,paint,night, drawTrack, null);
    }
    
    /**
     * This will draw the closed shape in canvas with given screen params
     * @param c
     * @param origin
     * @param scale
     * @param movement
     * @param paint
     */
	public void drawShape(Canvas c, Origin origin, Scale scale, Movement movement, Paint paint, boolean night, boolean drawTrack, Plan plan) {

        // TrackShape type is used for a flight plan destination
        if (this instanceof TrackShape) {
            
            /*
             * Draw background on track shapes, so draw twice. There is the
             * the possibility of a "future" leg being the same as the current or prev leg, as
             * would be the case if an approach came in from a VOR, then the missed approach goes
             * back to the same VOR, or a VOR used in a procedure turn. This can be handled by
             * cycling through the list twice and drawing them in 2 passes.
             *
             * Note there is NOT a 1:1 relationship between coord's and the legs of a plan. Each
             * coord has a property that indicates its leg within the plan, use that.
             */
        	int cMax = getNumCoords() - 1;
            int currentLeg = ((null == plan) ? 0 : plan.findNextNotPassed() - 1);

            // Pass 1 - draw all of the legs that are AFTER the current leg
            // We can start our search at currentLeg * 2 into the coord array due to each
            // plan leg having at least 2 coord entries, saves a bit of looping time.
            for(int coord = currentLeg * 2; coord < cMax; coord++) {
                if(mCoords.get(coord).getLeg() > currentLeg) {
                    drawPlanSegment(c, origin, paint, night, drawTrack, plan, coord);
                }
            }

            // Pass 2 - draw all of the legs that are before AND the current leg
            for(int coord = 0; coord < cMax; coord++) {
                if(mCoords.get(coord).getLeg() <= currentLeg) {
                    drawPlanSegment(c, origin, paint, night, drawTrack, plan, coord);
                } else break;   // If we're past current leg, stop the for() loop
            }

        } else {
            /*
             * Draw the shape segment by segment
             */
            if(getNumCoords() > 0) {
                float pts[] = new float[(getNumCoords()) * 4];
                int i = 0;
                int coord = 0;
                float x1 = (float) origin.getOffsetX(mCoords.get(coord).getLongitude());
                float y1 = (float) origin.getOffsetY(mCoords.get(coord).getLatitude());
                float x2;
                float y2;

                for (coord = 1; coord < getNumCoords(); coord++) {
                    x2 = (float) origin.getOffsetX(mCoords.get(coord).getLongitude());
                    y2 = (float) origin.getOffsetY(mCoords.get(coord).getLatitude());

                    pts[i++] = x1;
                    pts[i++] = y1;
                    pts[i++] = x2;
                    pts[i++] = y2;

                    x1 = x2;
                    y1 = y2;
                }
                c.drawLines(pts, paint);
            }
        }
    }

    // Draw the plan segment indicated by the coord index
    //
    void drawPlanSegment(Canvas c, Origin origin, Paint paint, boolean night, boolean drawTrack, Plan plan, int coord) {
        float x1 = (float)origin.getOffsetX(mCoords.get(coord).getLongitude());
        float x2 = (float)origin.getOffsetX(mCoords.get(coord + 1).getLongitude());
        float y1 = (float)origin.getOffsetY(mCoords.get(coord).getLatitude());
        float y2 = (float)origin.getOffsetY(mCoords.get(coord + 1).getLatitude());

        float width = paint.getStrokeWidth();
        int color = paint.getColor();

        if(drawTrack) {
            paint.setStrokeWidth(width + 4);
            paint.setColor(night? Color.WHITE : Color.BLACK);
            c.drawLine(x1, y1, x2, y2, paint);
            paint.setStrokeWidth(width);

            if(null == plan) {
                paint.setColor(color);
            } else {
                paint.setColor(TrackShape.getLegColor(plan, mCoords.get(coord).getLeg()));
            }
            c.drawLine(x1, y1, x2, y2, paint);
        }

        if(mCoords.get(coord + 1).isSeparate()) {
            paint.setColor(night? Color.WHITE : Color.BLACK);
            c.drawCircle(x2, y2, width + 8, paint);
            paint.setColor(Color.GREEN);
            c.drawCircle(x2, y2, width + 6, paint);
            paint.setColor(color);
        }
        if(mCoords.get(coord).isSeparate()) {
            paint.setColor(night? Color.WHITE : Color.BLACK);
            c.drawCircle(x1, y1, width + 8, paint);
            paint.setColor(Color.GREEN);
            c.drawCircle(x1, y1, width + 6, paint);
            paint.setColor(color);
        }
    }
    /*
     * Determine if shape belong to a screen based on Screen longitude and latitude
     * and shape max/min longitude latitude
     */
    public boolean isOnScreen(Origin origin){

        double maxLatScreen = origin.getLatScreenTop();
        double minLatScreen = origin.getLatScreenBot();
        double minLonScreen = origin.getLonScreenLeft();
        double maxLonScreen = origin.getLonScreenRight();

        boolean isInLat = mLatMin < maxLatScreen && mLatMax > minLatScreen;
        boolean isInLon = mLonMin < maxLonScreen && mLonMax > minLonScreen;
        return isInLat && isInLon;

    }

    /**
     * 
     * @return
     */
    public int getNumCoords() {
        return mCoords.size();
    }

    /**
     * 
     * @return
     */
    public double getLatitudeMinimum() {
        return mLatMin;
    }
    
    /**
     * 
     * @param lon
     * @param lat
     * @return
     */
    public String getTextIfTouched(double lon, double lat) {
        if(null == mPoly) {
            return null;
        }
        if(mPoly.contains(new Point((float)lon, (float)lat))) {
            return mText;
        }
        return null;
    }

    /**
     *
     */
    public String getLabel() {
        return mText;
    }
    
    /**
     * 
     */
    public void makePolygon() {
        if(getNumCoords() > 2) {
            mPoly = mPolyBuilder.build();
        }
    } 
}
