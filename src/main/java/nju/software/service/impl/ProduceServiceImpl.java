package nju.software.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import nju.software.dao.impl.AccessoryDAO;
import nju.software.dao.impl.FabricDAO;
import nju.software.dao.impl.LogisticsDAO;
import nju.software.dao.impl.OrderDAO;
import nju.software.dao.impl.QuoteDAO;
import nju.software.dataobject.Accessory;
import nju.software.dataobject.Account;
import nju.software.dataobject.Fabric;
import nju.software.dataobject.Logistics;
import nju.software.dataobject.Order;
import nju.software.dataobject.Quote;
import nju.software.model.QuoteConfirmTaskSummary;
import nju.software.model.SampleProduceTask;
import nju.software.model.SampleProduceTaskSummary;
import nju.software.service.ProduceService;
import nju.software.util.JbpmAPIUtil;

@Service("produceServiceImpl")
public class ProduceServiceImpl implements ProduceService {

	@Autowired
	private OrderDAO orderDAO;
	@Autowired
	private JbpmAPIUtil jbpmAPIUtil;
	@Autowired
	private LogisticsDAO logisticsDAO;
	@Autowired
	private FabricDAO fabricDAO;
	@Autowired
	private AccessoryDAO accessoryDAO;
	@Autowired
	private QuoteDAO quoteDAO;

	@Override
	public boolean verify(Account account, int orderId, long taskId,
			long processId, boolean productVal, String comment) {
		// TODO Auto-generated method stub
		// String actorId = account.getUserRole();
		String actorId = "SHENGCHANZHUGUAN";
		// 需要获取task中的数据
		WorkflowProcessInstance process = (WorkflowProcessInstance) jbpmAPIUtil
				.getKsession().getProcessInstance(processId);
		int orderId_process = (int) process.getVariable("orderId");
		System.out.println("orderId: " + orderId);
		if (orderId == orderId_process) {
			Order order = orderDAO.findById(orderId);
			// 修改order内容

			// 提交修改
			orderDAO.attachDirty(order);

			// 修改流程参数
			Map<String, Object> data = new HashMap<>();
			data.put("productVal", productVal);
			data.put("productComment", comment);
			// 直接进入到下一个流程时
			try {
				jbpmAPIUtil.completeTask(taskId, data, actorId);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	@Override
	public Logistics getLogisticsByOrderId(int orderId) {
		// TODO Auto-generated method stub
		Logistics logistic = logisticsDAO.findById(orderId);
		return logistic;
	}

	@Override
	public List<Fabric> getFabricByOrderId(int orderId) {
		// TODO Auto-generated method stub
		List<Fabric> list = fabricDAO.findByOrderId(orderId);
		return list;
	}

	@Override
	public List<Accessory> getAccessoryByOrderId(int orderId) {
		// TODO Auto-generated method stub
		List<Accessory> list = accessoryDAO.findByOrderId(orderId);
		return list;
	}

	@Override
	public List<SampleProduceTaskSummary> getSampleProduceTaskSummaryList() {
		// TODO Auto-generated method stub
		List<TaskSummary> tasks = jbpmAPIUtil.getAssignedTasksByTaskname(
				"SHENGCHANZHUGUAN", "product_sample");
		List<SampleProduceTaskSummary> taskSummarys = new ArrayList<>();
		for (TaskSummary task : tasks) {
			Integer orderId = (Integer) getVariable("orderId", task);
			SampleProduceTaskSummary summary = SampleProduceTaskSummary
					.getInstance(orderDAO.findById(orderId), (Quote) quoteDAO
							.findByProperty("order_id", orderId).get(0), task
							.getId());
			taskSummarys.add(summary);

		}
		return taskSummarys;
	}

	@Override
	public SampleProduceTask getSampleProduceTask(Integer orderId, long taskId) {
		// TODO Auto-generated method stub
		Order order = orderDAO.findById(orderId);
		List<Fabric> fabrics=fabricDAO.findByOrderId(orderId);
		List<Accessory> accessorys=accessoryDAO.findByOrderId(orderId);
		SampleProduceTask task = new SampleProduceTask(taskId, order, fabrics, accessorys);
		return task;
	}

	@Override
	public void completeSampleProduceTask(long taskId, String result) {
		// TODO Auto-generated method stub
		Map<String,Object> data=new HashMap<String,Object>();
		data.put("producterror", result.equals("0"));
		try {
			jbpmAPIUtil.completeTask(taskId, data, "SHENGCHANZHUGUAN");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Object getVariable(String name, TaskSummary task) {
		StatefulKnowledgeSession session = jbpmAPIUtil.getKsession();
		long processId = task.getProcessInstanceId();
		WorkflowProcessInstance process = (WorkflowProcessInstance) session
				.getProcessInstance(processId);
		return process.getVariable(name);
	}

}
