package pereca.pictogram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class FirstPage extends Activity {

	private Handler handler;

	private int SPLASH_LENGTH = 500;

	// single list and its adapter
	private ListView main_list;
	private PictogramAdapter adapter;
	private List<RowContainer> adapter_source;

	// category -> hash
	private Map<String, List<RowContainer>> resources;

	private List<String> categories;
	private Map<String, Integer> rows_numer;
	private int columns_number;

	private Map<String, String> transition_mapping;
	private String current_position;
	private String current_selected;

	private String current_played;

	private OnClickListener pictogram_click_listener;

	private boolean in_splash;

	private MediaPlayer player;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.splash_screen);

		this.in_splash = true;

		// handler for main looper
		this.handler = new Handler(this.getMainLooper());

		this.handler.postDelayed(new Runnable() {

			@Override
			public void run() {

				initPage();

			}
		}, this.SPLASH_LENGTH);

	}

	private void initPage() {

		// change layout, remove splash screen
		this.setContentView(R.layout.first_page_layout);

		this.in_splash = false;

		// load category and sizes "configuration"
		this.initConfig();

		// reference view from xml
		this.main_list = (ListView) this.findViewById(R.id.main_list);

		// populate resources map
		this.loadImages();

		this.adapter_source = this.getCategoryContent("naslovna");

		this.current_position = "naslovna";

		// initialize adapters
		this.adapter = new PictogramAdapter(this, R.layout.list_row, R.layout.single_pictogram,
				this.adapter_source, this.pictogram_click_listener);

		this.main_list.setAdapter(this.adapter);

	}

	private void initConfig() {

		// prepare categories
		this.categories = new ArrayList<String>();
		this.categories.add("naslovna");
		this.categories.add("hrana");
		this.categories.add("igre");
		this.categories.add("medicina");
		this.categories.add("povrce");
		this.categories.add("voce");
		this.categories.add("emotikon");

		// prepare their length
		this.rows_numer = new HashMap<String, Integer>();
		this.rows_numer.put("naslovna", 3);
		this.rows_numer.put("hrana", 5);
		this.rows_numer.put("igre", 5);
		this.rows_numer.put("medicina", 5);
		this.rows_numer.put("povrce", 5);
		this.rows_numer.put("voce", 5);
		this.rows_numer.put("emotikon", 5);

		// number of columns per page
		this.columns_number = 2;

		// define trasitions
		this.transition_mapping = new HashMap<String, String>();

		this.transition_mapping.put("naslovna-0-0", "medicina");
		this.transition_mapping.put("naslovna-0-1", "igre");
		this.transition_mapping.put("naslovna-1-0", "povrce");
		this.transition_mapping.put("naslovna-1-1", "voce");
		this.transition_mapping.put("naslovna-2-0", "hrana");
		this.transition_mapping.put("naslovna-2-1", "emotikon");

		this.pictogram_click_listener = new OnClickListener() {

			@Override
			public void onClick(View view) {

				// pictogram position in format: name_row_column
				String view_tag = (String) view.getTag();

				// int position = main_list.getVerticalScrollbarPosition();

				handleClickFrom(view_tag);

				((ArrayAdapter<RowContainer>) main_list.getAdapter()).notifyDataSetChanged();

				// main_list.setVerticalScrollbarPosition(position);

			}

		};

	}

	private void handleClickFrom(String source) {

		// category-row-column

		String[] infos = source.split("-");

		// if click was on mainPage
		if (infos[0].equals("naslovna")) {

			String next_category = this.transition_mapping.get(source);

			// change list_view dataSource
			this.adapter_source = this.getCategoryContent(next_category);

			this.adapter = new PictogramAdapter(this, R.layout.list_row, R.layout.single_pictogram,
					this.adapter_source, this.pictogram_click_listener);

			this.main_list.setAdapter(this.adapter);

			this.current_position = next_category;

		} else {

			this.playSound(infos[0] + "_" + infos[1] + "_" + infos[2]);

			if (this.current_selected != null) {

				// remove filter from selected

				this.restorePictogram(this.current_selected);

				this.current_selected = null;

			}

			// set filter to new one

			this.selectPictogram(Integer.parseInt(infos[1]), Integer.parseInt(infos[2]));

			this.current_selected = source;

		}

	}

	private void playSound(String resource) {

		int res_id = this.getResources().getIdentifier(resource, "raw", getPackageName());

		if (this.player == null) {

			this.player = MediaPlayer.create(this, res_id);

		} else {

			if (this.current_played.equals(resource)) {

				// click on the same icon

				if (this.player.isPlaying()) {

					this.player.pause();
					this.player.seekTo(0);
				}

			} else {

				// different icon

				if (this.player.isPlaying()) {
					this.player.stop();
					this.player.release();
				}

				this.player = MediaPlayer.create(this, res_id);

			}

		}

		player.start();

		this.current_played = resource;

	}

	private void restorePictogram(String pictogram) {

		String[] infos = pictogram.split("-");

		RowContainer row = this.adapter_source.get(Integer.parseInt(infos[1]));

		RowContainer original_row = ((List<RowContainer>) this.resources.get(infos[0]))
				.get(Integer.parseInt(infos[1]));

		row.replace(Integer.parseInt(infos[2]), original_row.getSingle_row().get(Integer.parseInt(infos[2])));

	}

	private void selectPictogram(int row, int column) {

		RowContainer row_container = this.adapter_source.get(row);

		row_container.replace(column, this.invertBitmap(row_container.getSingle_row().get(column)));

	}

	private List<RowContainer> getCategoryContent(String category) {

		List<RowContainer> temp_list = new ArrayList<RowContainer>();

		for (RowContainer row : this.resources.get(category)) {

			List<Bitmap> single_row = new ArrayList<Bitmap>();

			for (Bitmap bitmap : row.getSingle_row()) {

				single_row.add(bitmap.copy(bitmap.getConfig(), true));

			}

			temp_list.add(new RowContainer(single_row, category));
		}

		return temp_list;
	}

	@Override
	public void onBackPressed() {

		if (!this.in_splash && !this.current_position.equals("naslovna")) {

			// restore selected pictogram
			if (this.current_selected != null) {
				this.restorePictogram(this.current_selected);
				this.current_selected = null;
			}

			this.adapter_source = this.getCategoryContent("naslovna");
			this.adapter = new PictogramAdapter(this, R.layout.list_row, R.layout.single_pictogram,
					this.adapter_source, this.pictogram_click_listener);
			this.main_list.setAdapter(this.adapter);

			this.current_position = "naslovna";

		} else {
			// default action
			// exit from application
			super.onBackPressed();
		}

	}

	private Bitmap invertBitmap(Bitmap src) {

		int height = src.getHeight();
		int width = src.getWidth();

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();

		ColorMatrix matrixGrayscale = new ColorMatrix();
		matrixGrayscale.setSaturation(0);

		ColorMatrix matrixInvert = new ColorMatrix();
		matrixInvert.set(new float[] {	-1.0f,
										0.0f,
										0.0f,
										0.0f,
										255.0f,
										0.0f,
										-1.0f,
										0.0f,
										0.0f,
										255.0f,
										0.0f,
										0.0f,
										-1.0f,
										0.0f,
										255.0f,
										0.0f,
										0.0f,
										0.0f,
										1.0f,
										0.0f });
		matrixInvert.preConcat(matrixGrayscale);

		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixInvert);
		paint.setColorFilter(filter);

		canvas.drawBitmap(src, 0, 0, paint);

		return bitmap;

	}

	private void loadImages() {

		this.resources = new HashMap<String, List<RowContainer>>();

		// category_row_column -> pictogram name in resources

		for (String category : this.categories) {
			// for every category

			List<RowContainer> rows = new ArrayList<RowContainer>();

			for (int row = 0; row < this.rows_numer.get(category); row++) {
				// for each row in category

				List<Bitmap> single_row_list = new ArrayList<Bitmap>();
				for (int column = 0; column < this.columns_number; column++) {
					// for each column in row

					single_row_list.add(BitmapFactory
							.decodeResource(this.getResources(), this.getResourceId(category, row, column)));

				}
				rows.add(new RowContainer(single_row_list, category));

			}

			this.resources.put(category, rows);

		}

	}

	private int getResourceId(String category, int row, int column) {
		return this.getResources()
				.getIdentifier(category + "_" + row + "_" + column, "drawable", this.getPackageName());
	}

}
