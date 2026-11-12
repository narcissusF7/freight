package com.lukachangtong.freight.waybill.vo;

import com.lukachangtong.freight.waybill.entity.WbNode;
import com.lukachangtong.freight.waybill.entity.WbReceipt;
import com.lukachangtong.freight.waybill.entity.WbWaybill;

import java.util.ArrayList;
import java.util.List;

/**
 * 司机端运单详情：运单 + 节点时间轴 + 回单 + 可执行动作。
 *
 * 把「下一步能干什么」由服务端算好下发（nextAction / availableActions），
 * 前端只按 action 名调接口——否则状态机规则会被复制到 APP 里，两边必然漂移。
 */
public class DriverWaybillDetailVo {

    private WbWaybill waybill;

    /** 车牌号（司机端展示，免去再查一次车辆） */
    private String plateNo;

    private String contactName;
    private String contactPhone;

    /** 节点时间轴，按时间正序 */
    private List<WbNode> nodes = new ArrayList<WbNode>();

    private List<WbReceipt> receipts = new ArrayList<WbReceipt>();

    /** 下一步动作名（DriverNodeAction），null 表示当前状态司机无事可做 */
    private String nextAction;
    private String nextActionLabel;

    /** 当前所有可执行动作，前端渲染按钮用 */
    private List<ActionVo> availableActions = new ArrayList<ActionVo>();

    /** 距装货地/卸货地距离（米），前端在确认按钮旁提示「距装货地 320 米」 */
    private Integer distanceToLoadM;
    private Integer distanceToUnloadM;

    /** 运单当前状态的业务含义 */
    private String statusText;

    /** 货主/调度对本次操作的提示（例如定位超出范围被拒后的人工放行说明） */
    private String tip;

    public static class ActionVo {
        private String action;
        private String label;
        /** 是否必须先取定位 */
        private boolean locationRequired;

        public ActionVo() {
        }

        public ActionVo(String action, String label, boolean locationRequired) {
            this.action = action;
            this.label = label;
            this.locationRequired = locationRequired;
        }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public boolean isLocationRequired() { return locationRequired; }
        public void setLocationRequired(boolean locationRequired) { this.locationRequired = locationRequired; }
    }

    public WbWaybill getWaybill() { return waybill; }
    public void setWaybill(WbWaybill waybill) { this.waybill = waybill; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public List<WbNode> getNodes() { return nodes; }
    public void setNodes(List<WbNode> nodes) { this.nodes = nodes; }
    public List<WbReceipt> getReceipts() { return receipts; }
    public void setReceipts(List<WbReceipt> receipts) { this.receipts = receipts; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getNextActionLabel() { return nextActionLabel; }
    public void setNextActionLabel(String nextActionLabel) { this.nextActionLabel = nextActionLabel; }
    public List<ActionVo> getAvailableActions() { return availableActions; }
    public void setAvailableActions(List<ActionVo> availableActions) { this.availableActions = availableActions; }
    public Integer getDistanceToLoadM() { return distanceToLoadM; }
    public void setDistanceToLoadM(Integer distanceToLoadM) { this.distanceToLoadM = distanceToLoadM; }
    public Integer getDistanceToUnloadM() { return distanceToUnloadM; }
    public void setDistanceToUnloadM(Integer distanceToUnloadM) { this.distanceToUnloadM = distanceToUnloadM; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
}
