package gotapps.appresist;

/**
 * Created by Fenta Rizky on 09/12/2017.
 */
//Load Open CV library
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.widget.SeekBar;

import org.opencv.android.JavaCameraView;

import java.util.List;
public class ResistorCameraCapture extends JavaCameraView {
    public ResistorCameraCapture(Context context, int cameraId) {
        super(context, cameraId);
    }

    public ResistorCameraCapture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    //inisialisasi zoomcontrol
    protected SeekBar _zoomControl;
    public void setZoomControl(SeekBar zoomControl)
    {
        _zoomControl = zoomControl;
    }
    protected void enableZoomControls(Camera.Parameters params)
    {
        final SharedPreferences settings = getContext().getSharedPreferences("ZoomCtl", 0);

        // Set zoom level ke level Maximal apabila pengaturan zoom sebelumnya tidak ada.
        final int maxZoom = params.getMaxZoom();
        int currentZoom = settings.getInt("ZoomLvl", maxZoom);
        params.setZoom(currentZoom);

        if(_zoomControl == null) return;

        _zoomControl.setMax(maxZoom);
        _zoomControl.setProgress(currentZoom);
        _zoomControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Camera.Parameters params = mCamera.getParameters();
                params.setZoom(progress);
                mCamera.setParameters(params);

                if (settings != null)
                {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("ZoomLvl", progress);
                    editor.apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    // zoom in dan enable flash
    protected boolean initializeCamera(int width, int height)
    {
        boolean ret = super.initializeCamera(width, height);

        Camera.Parameters params = mCamera.getParameters();

        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        else if(FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }


        if(params.isZoomSupported())
            enableZoomControls(params);

        mCamera.setParameters(params);

        return ret;
    }
}
