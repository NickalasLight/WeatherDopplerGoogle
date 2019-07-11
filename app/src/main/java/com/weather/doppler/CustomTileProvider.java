package com.weather.doppler;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;

public class CustomTileProvider implements TileProvider {
    private long timeStamp;
    HashMap<String, byte[]> tileBitmapMap = new HashMap<String, byte[]>();
    CustomTileProvider(long timeStamp){
        this.timeStamp = timeStamp;
    }

    public Tile getTile(int x, int y, int zoom) {

        if(checkTileDownloaded(x,y,zoom,timeStamp)){
        //if tile has been downloaded to cache return the tile
            return new Tile(1,1, tileBitmapMap.get(""+x+y+zoom+timeStamp));
        }

        else
            {

                tileBitmapMap.put(""+x+y+zoom+timeStamp,downloadTileBitmap(x,y,zoom,timeStamp));
                //if the tile has not been downloaded, return no tile, but download tiles to cache.
                return  new Tile(1,1, tileBitmapMap.get(""+x+y+zoom+timeStamp));}
    //Tile newTile = downloadTiles(x,y,zoom,timeStamp);
    //return newTile;
    }

    private byte[] downloadTileBitmap(int x, int y, int zoom, long timeStamp){
try {

    URL url = getTileUrl(x, y, zoom, timeStamp);
    InputStream inputStream = (InputStream) url.getContent();
    Bitmap tileBitmap = BitmapFactory.decodeStream(inputStream);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    tileBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

    //Tile newTile = new Tile(1,1,stream.toByteArray());

    return stream.toByteArray();
}
catch(Exception ex){
    ex.printStackTrace();
}
Tile newTile = new Tile(1,1,new byte[1]);
return new byte[1];

}
    private boolean checkTileDownloaded(int x,int y, int zoom,long timeStamp){
        return tileBitmapMap.containsKey(""+x+y+zoom+timeStamp);}

    private URL getTileUrl(int x, int y, int zoom,long timeStamp){
        try {
        String s = String.format("https://tilecache.rainviewer.com/v2/radar/%d/256/%d/%d/%d/2/1_1.png",
                timeStamp,zoom, x, y);

            return new URL(s);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }

    }
}
