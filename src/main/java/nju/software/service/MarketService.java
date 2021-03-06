package nju.software.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.task.Task;
import org.springframework.transaction.annotation.Transactional;

import nju.software.dataobject.Accessory;
import nju.software.dataobject.Account;
import nju.software.dataobject.Customer;
import nju.software.dataobject.DesignCad;
import nju.software.dataobject.Employee;
import nju.software.dataobject.Fabric;
import nju.software.dataobject.Logistics;
import nju.software.dataobject.MarketstaffAlter;
import nju.software.dataobject.Money;
import nju.software.dataobject.Order;
import nju.software.dataobject.Produce;
import nju.software.dataobject.Product;
import nju.software.dataobject.Quote;
import nju.software.dataobject.VersionData;
import nju.software.model.OrderInfo;
import nju.software.model.OrderModel;
import nju.software.model.ProductModel;

public interface MarketService {
	/**
	 * 市场秘书分配订单
	 * @param order
	 */
	@Transactional(rollbackFor = Exception.class)
	public void assignCustomerOrder(Order order);
	
	/**
	 * 市场专员认领客户订单
	 * @param order;
	 * @param userId;
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean claimCustomerOrder(Integer orderId, Integer employeeId);
	
	/**
	 * 搜索未被认领的订单列表
	 * @param ordernumber
	 * @param customername
	 * @param stylename
	 * @param startdate
	 * @param enddate
	 * @return 
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchTodoOrderList(String ordernumber,String customername,String stylename,String startdate,String enddate);
	
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>>  getAlterInfoByOrderId(Integer orderId);
	
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object>  getAlterInfoByAlterId(Integer alterId);
	
	/**
	 * 获取所有未审批的申请和理由
	 * @return 返回申请信息对象和理由
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>>  getAlltoDoAlterInfo();
	       

	/**
	 *提交ApplyAlter审批
	 *@param alter 申请信息
	 *@param taskId 
	 *@param processId
	 *@param result
	 *@param suggestion
	 */
	@Transactional(rollbackFor = Exception.class)
	public void verifyAlterSubmit(MarketstaffAlter alter, String taskId,
			String processId, boolean result, String suggestion) throws InterruptedException;
	
	/**
	 * 获取所有未审批的申请
	 * @return 未审批的申请列表
	 */	
	@Transactional(rollbackFor = Exception.class)
	public List<MarketstaffAlter> getAlltoDoAlter();
	
	
	/**
	 * 根据id获取申请信息
	 * @param alterId
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public MarketstaffAlter getMarketStaffAlterById(int alterId);
	
	
	/**
	 * 申请更换专员
	 * @param alterInfo 申请信息
	 * @param reason 申请理由
	 * @param processId 被申请订单的流程实例ID
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean applyForAlterMarketStaffSubmit(MarketstaffAlter alterInfo, String reason, String processId);
	
	/**
	 * 专员查看当前正在审批中的订单列表
	 * @param actorId 专员ID
	 * @return
	 */
	public List<Map<String, Object>> getOrderAlterDoingList(String actorId);
	
		
	/**
	 * 获取专员变更申请详细信息
	 * @param alterId 专员变更记录的id
	 * @return
	 */
	public Map<String, Object> getApplyAlterInfo(Integer alterId);

	/**
	 * 查看专员申请列表
	 * @param actorId 市场主管ID
	 * @return 
	 */
	public List<Map<String, Object>> getApplyAlterOrderList(String actorId);
	
	
	
	

	// ==========================报价商定=======================
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getConfirmQuoteList(String actorId);

	@Transactional(rollbackFor = Exception.class)
	public OrderInfo getConfirmQuoteDetail(String arctorId, Integer orderId);

	@Transactional(rollbackFor = Exception.class)
	public boolean confirmQuoteSubmit(String actorId, String taskId, String result);

	// ==========================签订合同=======================
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSignContractList(String actorId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getSignContractDetail(String arctorId,
			Integer orderId);

	@Transactional(rollbackFor = Exception.class)
	public void signContractSubmit(String actorId, String taskId, int i,
			double d, double discount, String url);

	@Transactional(rollbackFor = Exception.class)
	public List<Customer> getAddOrderList();

	@Transactional(rollbackFor = Exception.class)
	public Customer getAddOrderDetail(Integer cid);

	@Transactional(rollbackFor = Exception.class)
	public boolean addOrderSubmit(Order order, List<Fabric> fabrics,
			List<Accessory> accessorys, Logistics logistics,
			List<Produce> produces, List<Produce> sample_produces,
			List<VersionData> versions, DesignCad cad,
			HttpServletRequest request);

	// public void completeQuoteConfirmTaskSummary(String taskId,String result);

	@Transactional(rollbackFor = Exception.class)
	public List<Product> getProductList(int orderId, String productAskAmount,
			String productColor, String productStyle);

	@Transactional(rollbackFor = Exception.class)
	public boolean confirmProduceOrderSubmit(String string, int orderId,
			String taskId, String processId, boolean comfirmworksheet,
			List<Produce> produces);

	@Transactional(rollbackFor = Exception.class)
	public OrderInfo getOrderInfo(Integer orderId, String taskId);

	// public void completeSignContract(Integer orderId,double discount,String
	// taskId);

	// public List<QuoteConfirmTaskSummary>
	// getQuoteModifyTaskSummaryList(Integer employeeId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getModifyProductList(Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public boolean modifyProductSubmit(String string, int id, String taskId,
			String processId, boolean b, List<Produce> produces);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getModifyQuoteList(Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getModifyQuoteDetail(int id, int accountId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getModifyProductDetail(int id, Integer integer);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getMergeQuoteList(Integer accountId);

	@Transactional(rollbackFor = Exception.class)
	public void mergeQuoteSubmit(int accountId, Quote quote, int id,
			String taskId, String processId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getVerifyQuoteList(Integer accountId);

	@Transactional(rollbackFor = Exception.class)
	public void verifyQuoteSubmit(Quote quote, int id, String taskId,
			String processId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getModifyOrderList(Integer accountId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getModifyOrderDetail(int accountId, int id);

	@Transactional(rollbackFor = Exception.class)
	public void modifyOrderSubmit(Order order, List<Fabric> fabrics,
			List<Accessory> accessorys, Logistics logistics,
			List<Produce> produces, List<Produce> sample_produces,
			List<VersionData> versions, DesignCad cad, boolean editok,
			String taskId, Integer accountId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getMergeQuoteDetail(Integer userId, int id);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getVerifyQuoteDetail(Integer userId, int id);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getConfirmQuoteDetail(Integer userId, int id);

	@Transactional(rollbackFor = Exception.class)
	public void modifyQuoteSubmit(Quote quote, int id, String taskId,
			String processId, Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getConfirmProductList(String actorId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getConfirmProductDetail(Integer userId, int id);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrderList(Integer page);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getOrderDetail(Integer orderId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getAddMoreOrderList(int customerId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchAddMoreOrderList(
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getAddMoreOrderDetail(int id);

	@Transactional(rollbackFor = Exception.class)
	public boolean addMoreOrderSubmit(Order order, List<Fabric> fabrics,
			List<Accessory> accessorys, Logistics logistics,
			List<Produce> produces, List<VersionData> versions, DesignCad cad,
			HttpServletRequest request);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchOrderList(String ordernumber,
			String customername, String stylename, String startdate,
			String enddate, Integer[] employeeIds, String userRole,
			Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchModifyOrderList(Integer userId,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchMergeQuoteList(Integer userId,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchVerifyQuoteList(Integer userId,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchConfirmQuoteList(String string,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchModifyQuoteList(Integer userId,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchConfirmProductList(
			String actorId, String ordernumber, String customername,
			String stylename, String startdate, String enddate,
			Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchModifyProductList(Integer userId,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchSignContractList(String actorId,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrders();

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrders(String userRole, Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrdersDoing();

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrdersDoing(String userRole,
			Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrdersDone();

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchOrdersDoing(String ordernumber,String orderProcessStateName,
			String customername, String stylename, String startdate,
			String enddate, Integer[] employeeIds, String userRole,
			Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchOrdersDone(String ordernumber,
			String customername, String stylename, String startdate,
			String enddate, Integer[] employeeIds, String userRole,
			Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrdersDone(String userRole,
			Integer userId);

	@Transactional(rollbackFor = Exception.class)
	public void sendOrderInfoViaEmail(Order order, Customer customer);

	@Transactional(rollbackFor = Exception.class)
	public void sendOrderInfoViaPhone(Order order, Customer customer);

	@Transactional(rollbackFor = Exception.class)
	public ArrayList<String> getProcessStateName(Integer orderId);

	@Transactional(rollbackFor = Exception.class)
	public void signContractSubmit(String actorId, String parseString,
			int parseInt, double parseDouble, double parseDouble2, String url,
			String confirmDepositFileUrl, String moneyremark);

	@Transactional(rollbackFor = Exception.class)
	public boolean confirmQuoteSubmit(String actorId, String parseString,
			int parseInt, String result, String url, String moneyremark);

	@Transactional(rollbackFor = Exception.class)
	public boolean signConfirmFinalPaymentFileSubmit(int orderId,
			String confirmFinalPaymentFileUrl, String moneyremark);

	@Transactional(rollbackFor = Exception.class)
	public void verifyQuoteSubmit(Quote quote, int id, String taskId,
			String processId, boolean result, String comment);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getPushRestOrderList(String userId);

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getPushRestOrderDetail(String userId, int orderId);

	@Transactional(rollbackFor = Exception.class)
	public boolean getPushRestOrderSubmit(String actorId, String taskId,
			boolean result);

	@Transactional(rollbackFor = Exception.class)
	public boolean getPushRestOrderSubmit(String actorId, String taskId,
			boolean result, String orderId_string);

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getSearchPushRestOrderList(String string,
			String ordernumber, String customername, String stylename,
			String startdate, String enddate, Integer[] employeeIds);
	
	@Transactional(rollbackFor = Exception.class)
	public void testPrecondition(String userId, String taskName);
	
	@Transactional(rollbackFor = Exception.class)
	public String getComment(Object task, String variableName);

	/**
	 * 用户自主下单提交订单
	 * @param order
	 * @param fabrics
	 * @param accessorys
	 * @param logistics
	 * @param produces
	 * @param sample_produces
	 * @param versions
	 * @param cad
	 * @param request
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean addOrderCustomerSubmit(Order order, List<Fabric> fabrics,
			List<Accessory> accessorys, Logistics logistics,
			List<Produce> produces, List<Produce> sample_produces,
			List<VersionData> versions, DesignCad cad,
			HttpServletRequest request);
	/**
	 * 获取没有专员认领的订单
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrdersTodo();
	
	/**
	 * 用户自主下翻单提交订单
	 * @param order
	 * @param fabrics
	 * @param accessorys
	 * @param logistics
	 * @param produces
	 * @param versions
	 * @param cad
	 * @param request
	 */
	@Transactional(rollbackFor = Exception.class)
	public void addMoreCustomerOrderSubmit(Order order, List<Fabric> fabrics,
			List<Accessory> accessorys, Logistics logistics,
			List<Produce> produces, List<VersionData> versions, DesignCad cad,
			HttpServletRequest request);

	/**
	 * 根据客户id返回该用户未被认领的订单
	 * @param customerId 客户id
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> getOrdersTodo(Integer customerId);

}
