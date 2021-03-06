package com.acgist.snail.gui.javafx.window.setting;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import com.acgist.snail.config.DownloadConfig;
import com.acgist.snail.config.SystemConfig;
import com.acgist.snail.gui.javafx.Choosers;
import com.acgist.snail.gui.javafx.Controller;
import com.acgist.snail.gui.javafx.Desktops;
import com.acgist.snail.gui.javafx.Tooltips;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

/**
 * <p>设置窗口控制器</p>
 * 
 * @author acgist
 */
public final class SettingController extends Controller implements Initializable {
	
//	private static final Logger LOGGER = LoggerFactory.getLogger(SettingController.class);

	/**
	 * <p>下载速度滑块滑动大小：{@value}</p>
	 * <p>超过这个值时滑动必须是该值的整数倍</p>
	 */
	private static final int STEP_WIDTH = 512;
	
	@FXML
	private FlowPane root;
	@FXML
	private ScrollPane scrollPane;
	
	@FXML
	private Label pathValue;
	@FXML
	private Slider size;
	@FXML
	private Slider buffer;
	@FXML
	private Slider memoryBuffer;
	@FXML
	private CheckBox notice;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// 绑定宽高
		this.scrollPane.prefWidthProperty().bind(this.root.widthProperty());
		this.scrollPane.prefHeightProperty().bind(this.root.heightProperty());
		// 初始化
		this.initSetting();
		this.initControl();
	}

	/**
	 * <p>下载目录</p>
	 * 
	 * @param event 事件
	 */
	@FXML
	public void handlePathAction(ActionEvent event) {
		final File file = Choosers.chooseDirectory(SettingWindow.getInstance().stage(), "下载目录");
		if (file != null) {
			final String path = file.getAbsolutePath();
			DownloadConfig.setPath(path);
			this.pathValue(DownloadConfig.getPath());
		}
	}

	/**
	 * <p>消息提示</p>
	 * 
	 * @param event 事件
	 */
	@FXML
	public void handleNoticeAction(ActionEvent event) {
		DownloadConfig.setNotice(this.notice.isSelected());
	}
	
	/**
	 * <p>初始化配置</p>
	 */
	private void initSetting() {
		this.pathValue(DownloadConfig.getPath());
		this.size.setValue(DownloadConfig.getSize());
		this.buffer.setValue(DownloadConfig.getBuffer());
		this.memoryBuffer.setValue(DownloadConfig.getMemoryBuffer());
		this.notice.setSelected(DownloadConfig.getNotice());
	}
	
	/**
	 * <p>初始化控件</p>
	 */
	private void initControl() {
		// 初始化下载目录
		this.pathValue.setCursor(Cursor.HAND);
		this.pathValue.setOnMouseClicked(this.pathClickedAction);
		// 初始化任务数量
		this.size.valueProperty().addListener(this.sizeListener);
		this.size.setOnMouseReleased(this.sizeReleaseAction);
		// 初始化下载速度
		this.buffer.valueProperty().addListener(this.bufferListener);
		this.buffer.setOnMouseReleased(this.bufferReleaseAction);
		this.buffer.setLabelFormatter(this.bufferFormatter);
		// 初始化磁盘缓存
		this.memoryBuffer.valueProperty().addListener(this.memoryBufferListener);
		this.memoryBuffer.setOnMouseReleased(this.memoryBufferReleaseAction);
		this.memoryBuffer.setLabelFormatter(this.memoryBufferFormatter);
	}
	
	/**
	 * <p>下载目录事件</p>
	 */
	private EventHandler<MouseEvent> pathClickedAction = event -> {
		final File file = new File(DownloadConfig.getPath());
		Desktops.open(file);
	};

	/**
	 * <p>下载任务数量监听</p>
	 */
	private ChangeListener<? super Number> sizeListener = (obs, oldVal, newVal) -> {
		int value = newVal.intValue(); // 设置整数任务
		if(value <= 0) { // 不能设置：0
			value = 1;
		}
		this.size.setValue(value);
	};
	
	/**
	 * <p>下载任务数量保存</p>
	 */
	private EventHandler<MouseEvent> sizeReleaseAction = event -> {
		final Double value = this.size.getValue();
		DownloadConfig.setSize(value.intValue());
	};
	
	/**
	 * <p>下载速度监听</p>
	 */
	private ChangeListener<? super Number> bufferListener = (obs, oldVal, newVal) -> {
		int value = newVal.intValue();
		if(value < SystemConfig.MIN_DOWNLOAD_BUFFER_KB) {
			// 最小下载速度
			value = SystemConfig.MIN_DOWNLOAD_BUFFER_KB;
		} else if(value > STEP_WIDTH) {
			// 超过滑块大小时设置为滑块大小的整数倍
			value = value / STEP_WIDTH * STEP_WIDTH;
		}
		this.buffer.setValue(value);
	};
	
	/**
	 * <p>下载速度保存</p>
	 */
	private EventHandler<MouseEvent> bufferReleaseAction = event -> {
		final Double value = this.buffer.getValue();
		DownloadConfig.setBuffer(value.intValue());
	};
	
	/**
	 * <p>下载速度格式</p>
	 */
	private StringConverter<Double> bufferFormatter = new StringConverter<Double>() {
		@Override
		public String toString(Double value) {
			return (value.intValue() / SystemConfig.DATA_SCALE) + "M";
		}
		@Override
		public Double fromString(String label) {
			return Double.valueOf(label.substring(0, label.length() - 1)) * SystemConfig.DATA_SCALE;
		}
	};
	
	/**
	 * <p>磁盘缓存监听</p>
	 */
	private ChangeListener<? super Number> memoryBufferListener = (obs, oldVal, newVal) -> {
		final int value = newVal.intValue();
		this.memoryBuffer.setValue(value);
	};
	
	/**
	 * <p>磁盘缓存保存</p>
	 */
	private EventHandler<MouseEvent> memoryBufferReleaseAction = event -> {
		final Double value = this.memoryBuffer.getValue();
		DownloadConfig.setMemoryBuffer(value.intValue());
	};
	
	/**
	 * <p>磁盘缓存格式</p>
	 */
	private StringConverter<Double> memoryBufferFormatter = new StringConverter<Double>() {
		@Override
		public String toString(Double value) {
			return value.intValue() + "M";
		}
		@Override
		public Double fromString(String label) {
			return Double.valueOf(label.substring(0, label.length() - 1));
		}
	};
	
	/**
	 * <p>设置下载路径</p>
	 * 
	 * @param path 下载路径
	 */
	private void pathValue(String path) {
		this.pathValue.setText(path);
		this.pathValue.setTooltip(Tooltips.newTooltip(path));
	}
	
}
