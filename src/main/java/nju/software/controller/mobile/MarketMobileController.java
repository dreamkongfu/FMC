package nju.software.controller.mobile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;
import nju.software.dao.impl.DeliveryRecordDAO;
import nju.software.dataobject.Accessory;
import nju.software.dataobject.Account;
import nju.software.dataobject.Customer;
import nju.software.dataobject.DesignCad;
import nju.software.dataobject.Employee;
import nju.software.dataobject.Fabric;
import nju.software.dataobject.Logistics;
import nju.software.dataobject.MarketstaffAlter;
import nju.software.dataobject.Order;
import nju.software.dataobject.Produce;
import nju.software.dataobject.Quote;
import nju.software.dataobject.SearchInfo;
import nju.software.dataobject.VersionData;
import nju.software.service.BuyService;
import nju.software.service.CommonService;
import nju.software.service.CustomerService;
import nju.software.service.DesignCadService;
import nju.software.service.EmployeeService;
import nju.software.service.LogisticsService;
import nju.software.service.MarketService;
import nju.software.service.OrderService;
import nju.software.service.QuoteService;
import nju.software.service.impl.BuyServiceImpl;
import nju.software.service.impl.DesignServiceImpl;
import nju.software.service.impl.MarketServiceImpl;
import nju.software.service.impl.ProduceServiceImpl;
import nju.software.util.Constants;
import nju.software.util.DateUtil;
import nju.software.util.FileOperateUtil;
import nju.software.util.ImageUtil;
import nju.software.util.JSONUtil;
import nju.software.util.JavaMailUtil;
import nju.software.util.ListUtil;
import nju.software.util.SessionUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Controller
public class MarketMobileController {
	private final static String UPLOAD_DIR = "upload_new";
	private final static String CONTRACT_URL = "/upload_new/contract/";// 合同图片
	private final static String CONFIRM_SAMPLEMONEY_URL = "/upload_new/confirmSampleMoneyFile/";// 样衣金收取钱款图片
	private final static String CONFIRM_DEPOSIT_URL = "/upload_new/confirmDepositFile/";// 大货首定金收取钱款图片
	private final static String CONFIRM_FINALPAYMENT_URL = "/upload_new/confirmFinalPaymentFile/";// 大货首定金收取钱款图片

	@Autowired
	private OrderService orderService;
	@Autowired
	private BuyService buyService;
	@Autowired
	private DesignCadService cadService;
	@Autowired
	private LogisticsService logisticsService;
	@Autowired
	private QuoteService quoteService;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private DeliveryRecordDAO deliveryRecordDAO;
	@Autowired
	private JavaMailUtil javaMailUtil;
	@Autowired
	private EmployeeService employeeService;
	@Autowired
	private CommonService commonService;
	@Autowired
	private JSONUtil jsonUtil;
	
	// ================================客户下单====================================
	@RequestMapping(value = "/market/mobile_addOrder.do")
	// @Transactional(rollbackFor = Exception.class)
	public void addOrder(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		List<Customer> customers = new ArrayList<>();
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Customer customer = marketService
				.getAddOrderDetail(account.getUserId());
		customers.add(customer);
		model.addAttribute(Constants.JSON_CUSTOMERS, customers);
		jsonUtil.sendJson(response, model);
	}

	// ================================客户下单====================================
	@RequestMapping(value = "/market/mobile_addOrderList.do")
	// @Transactional(rollbackFor = Exception.class)
	public void addOrderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Customer> customers;
		if ("CUSTOMER".equals(account.getUserRole())) {
			customers = new ArrayList();
			Customer customer = marketService
					.getAddOrderDetail(account.getUserId());
			customers.add(customer);
		}
		else {
			customers = marketService.getAddOrderList();
		}
		model.addAttribute(Constants.JSON_CUSTOMERS, customers);
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_addOrderDetail.do")
	public void addOrderDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String cid = request.getParameter("cid");
		Customer customer = marketService.getAddOrderDetail(Integer
				.parseInt(cid));
		model.addAttribute(Constants.JSON_CUSTOMER, customer);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		if (Constants.USER_ROLE_CUSTOMER.equals(account.getUserRole())){
			List<Employee> employeeList = employeeService.getAllManagerStaff();
			//放在列表第一个，表明不指定市场专员
			Employee defaultNoEmployee = new Employee();
			defaultNoEmployee.setEmployeeId(-1);
			defaultNoEmployee.setEmployeeName("不指定专员");
			employeeList.add(0, defaultNoEmployee);
			model.addAttribute(Constants.JSON_EMPLOYEE_LIST, employeeList);
		}
		else
			model.addAttribute(Constants.JSON_EMPLOYEE_NAME, account.getNickName());
		jsonUtil.sendJson(response, model);
	}

	// -----------------提交订单数据---------------------------------
	@RequestMapping(value = "/market/mobile_addOrderSubmit.do")
	// @Transactional(rollbackFor = Exception.class)
	public void addOrderSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		// 订单数据
		Integer customerId = Integer.parseInt(request
				.getParameter("customerId"));
		Customer customer = customerService.findByCustomerId(customerId);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Integer employeeId = account.getUserId();
		String orderState = "A";
		Timestamp orderTime = new Timestamp(new Date().getTime());
		String customerName = customer.getCustomerName();
		String customerCompany = customer.getCompanyName();
		String customerCompanyFax = customer.getCompanyFax();
		String customerPhone1 = customer.getContactPhone1();
		String customerPhone2 = customer.getContactPhone2();
		String customerCompanyAddress = customer.getCompanyAddress();
		String styleName = request.getParameter("style_name");
		String fabricType = request.getParameter("fabric_type");
		String clothesType = request.getParameter("clothes_type");
		String styleSex = request.getParameter("style_sex");
		String styleSeason = request.getParameter("style_season");
		String specialProcess = StringUtils.join(
				request.getParameterValues("special_process"), "|");
		String otherRequirements = StringUtils.join(
				request.getParameterValues("other_requirements"), "|");
		String referenceUrl = request.getParameter("reference_url");
		Integer askAmount = Integer
				.parseInt(request.getParameter("ask_amount"));
		String askProducePeriod = request.getParameter("ask_produce_period");
		String ask_deliver_date = request.getParameter("ask_deliver_date");
		Timestamp askDeliverDate = getAskDeliverDateTime(ask_deliver_date);
		String askCodeNumber = request.getParameter("ask_code_number");
		Short hasPostedSampleClothes = Short.parseShort(request
				.getParameter("has_posted_sample_clothes"));
		Short isNeedSampleClothes = Short.parseShort(request
				.getParameter("is_need_sample_clothes"));
		String orderSource = request.getParameter("order_source");
		String sampleClothesPicture = request
				.getParameter("sample_clothes_picture");
		String refPicture = request.getParameter("reference_picture");
		// 面料数据
		String fabric_names = request.getParameter("fabric_name");
		String fabric_amounts = request.getParameter("fabric_amount");
		String fabric_name[] = fabric_names.split(",");
		String fabric_amount[] = fabric_amounts.split(",");
		List<Fabric> fabrics = new ArrayList<Fabric>();
		for (int i = 0; i < fabric_name.length; i++) {
			if (fabric_name[i].equals(""))
				continue;
			fabrics.add(new Fabric(0, fabric_name[i], fabric_amount[i]));
		}

		// 辅料数据
		String accessory_names = request.getParameter("accessory_name");
		String accessory_querys = request.getParameter("accessory_query");
		String accessory_name[] = accessory_names.split(",");
		String accessory_query[] = accessory_querys.split(",");
		List<Accessory> accessorys = new ArrayList<Accessory>();
		for (int i = 0; i < accessory_name.length; i++) {
			if (accessory_name[i].equals(""))
				continue;
			accessorys.add(new Accessory(0, accessory_name[i],
					accessory_query[i]));
		}

		// 大货加工要求
		String produce_colors = request.getParameter("produce_color");
		String produce_xss = request.getParameter("produce_xs");
		String produce_ss = request.getParameter("produce_s");
		String produce_ms = request.getParameter("produce_m");
		String produce_ls = request.getParameter("produce_l");
		String produce_xls = request.getParameter("produce_xl");
		String produce_xxls = request.getParameter("produce_xxl");
		String produce_js = request.getParameter("produce_j");
		String produce_color[] = produce_colors.split(",");
		String produce_xs[] = produce_xss.split(",");
		String produce_s[] = produce_ss.split(",");
		String produce_m[] = produce_ms.split(",");
		String produce_l[] = produce_ls.split(",");
		String produce_xl[] = produce_xls.split(",");
		String produce_xxl[] = produce_xxls.split(",");
		String produce_j[] = produce_js.split(",");
		List<Produce> produces = new ArrayList<Produce>();
		for (int i = 0; i < produce_color.length; i++) {
			if (produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(produce_color[i]);
			p.setOid(0);

			int l = Integer.parseInt(produce_l[i]);
			int m = Integer.parseInt(produce_m[i]);
			int s = Integer.parseInt(produce_s[i]);
			int xs = Integer.parseInt(produce_xs[i]);
			int xl = Integer.parseInt(produce_xl[i]);
			int xxl = Integer.parseInt(produce_xxl[i]);
			int j = Integer.parseInt(produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setProduceAmount(l + m + s + xs + xl + xxl + j);
			p.setType(Produce.TYPE_PRODUCE);
			produces.add(p);
		}

		// 样衣加工要求
		String sample_produce_colors = request
				.getParameter("sample_produce_color");
		String sample_produce_xss = request.getParameter("sample_produce_xs");
		String sample_produce_ss = request.getParameter("sample_produce_s");
		String sample_produce_ms = request.getParameter("sample_produce_m");
		String sample_produce_ls = request.getParameter("sample_produce_l");
		String sample_produce_xls = request.getParameter("sample_produce_xl");
		String sample_produce_xxls = request.getParameter("sample_produce_xxl");
		String sample_produce_js = request.getParameter("sample_produce_j");
		String sample_produce_color[] = sample_produce_colors.split(",");
		String sample_produce_xs[] = sample_produce_xss.split(",");
		String sample_produce_s[] = sample_produce_ss.split(",");
		String sample_produce_m[] = sample_produce_ms.split(",");
		String sample_produce_l[] = sample_produce_ls.split(",");
		String sample_produce_xl[] = sample_produce_xls.split(",");
		String sample_produce_xxl[] = sample_produce_xxls.split(",");
		String sample_produce_j[] = sample_produce_js.split(",");
		List<Produce> sample_produces = new ArrayList<Produce>();
		int sample_amount = 0;
		for (int i = 0; i < sample_produce_color.length; i++) {
			if (sample_produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(sample_produce_color[i]);
			p.setOid(0);
			int l = Integer.parseInt(sample_produce_l[i]);
			int m = Integer.parseInt(sample_produce_m[i]);
			int s = Integer.parseInt(sample_produce_s[i]);
			int xs = Integer.parseInt(sample_produce_xs[i]);
			int xl = Integer.parseInt(sample_produce_xl[i]);
			int xxl = Integer.parseInt(sample_produce_xxl[i]);
			int j = Integer.parseInt(sample_produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setType(Produce.TYPE_SAMPLE_PRODUCE);
			int temp = l + m + s + xs + xl + xxl + j;
			p.setProduceAmount(temp);
			sample_amount += temp;
			sample_produces.add(p);
		}

		// 版型数据
		String version_sizes = request.getParameter("version_size");
		String version_centerBackLengths = request
				.getParameter("version_centerBackLength");
		String version_busts = request.getParameter("version_bust");
		String version_waistLines = request.getParameter("version_waistLine");
		String version_shoulders = request.getParameter("version_shoulder");
		String version_buttocks = request.getParameter("version_buttock");
		String version_hems = request.getParameter("version_hem");
		String version_trouserss = request.getParameter("version_trousers");
		String version_skirts = request.getParameter("version_skirt");
		String version_sleevess = request.getParameter("version_sleeves");
		String version_size[] = version_sizes.split(",");
		String version_centerBackLength[] = version_centerBackLengths
				.split(",");
		String version_bust[] = version_busts.split(",");
		String version_waistLine[] = version_waistLines.split(",");
		String version_shoulder[] = version_shoulders.split(",");
		String version_buttock[] = version_buttocks.split(",");
		String version_hem[] = version_hems.split(",");
		String version_trousers[] = version_trouserss.split(",");
		String version_skirt[] = version_skirts.split(",");
		String version_sleeves[] = version_sleevess.split(",");
		List<VersionData> versions = new ArrayList<VersionData>();
		for (int i = 0; i < version_size.length; i++) {
			if (version_size[i].equals(""))
				continue;
			versions.add(new VersionData(0, version_size[i],
					version_centerBackLength[i], version_bust[i],
					version_waistLine[i], version_shoulder[i],
					version_buttock[i], version_hem[i], version_trousers[i],
					version_skirt[i], version_sleeves[i]));
		}

		// 物流数据
		Logistics logistics = new Logistics();
		if (hasPostedSampleClothes == 1) {
			String in_post_sample_clothes_time = request
					.getParameter("in_post_sample_clothes_time");
			String in_post_sample_clothes_type = request
					.getParameter("in_post_sample_clothes_type");
			String in_post_sample_clothes_number = request
					.getParameter("in_post_sample_clothes_number");

			logistics
					.setInPostSampleClothesTime(getAskDeliverDateTime(in_post_sample_clothes_time));

			logistics.setInPostSampleClothesType(in_post_sample_clothes_type);
			logistics
					.setInPostSampleClothesNumber(in_post_sample_clothes_number);
		}
		// if (isNeedSampleClothes == 1) {
		// String sample_clothes_time = request
		// .getParameter("sample_clothes_time");
		// String sample_clothes_type = request
		// .getParameter("sample_clothes_type");
		// String sample_clothes_number = request
		// .getParameter("sample_clothes_number");
		String sample_clothes_name = request
				.getParameter("sample_clothes_name");
		String sample_clothes_phone = request
				.getParameter("sample_clothes_phone");
		String sample_clothes_address = request
				.getParameter("sample_clothes_address");
		String sample_clothes_remark = request
				.getParameter("sample_clothes_remark");

		// logistics.setSampleClothesTime(getTime(sample_clothes_time));
		// logistics.setSampleClothesType(sample_clothes_type);
		// logistics.setSampleClothesNumber(sample_clothes_number);
		logistics.setSampleClothesName(sample_clothes_name);
		logistics.setSampleClothesPhone(sample_clothes_phone);
		logistics.setSampleClothesAddress(sample_clothes_address);
		logistics.setSampleClothesRemark(sample_clothes_remark);
		// }

		// CAD
		DesignCad cad = new DesignCad();
		cad.setOrderId(0);
		cad.setCadVersion((short) 1);
		String cad_fabric = request.getParameter("cadFabric");
		String cad_box = request.getParameter("cadBox");
		String cad_package = request.getParameter("cadPackage");
		String cad_version_data = request.getParameter("cadVersionData");
		String cad_tech = request.getParameter("cadTech");
		String cad_other = request.getParameter("cadOther");
		cad.setCadBox(cad_box);
		cad.setCadFabric(cad_fabric);
		cad.setCadOther(cad_other);
		cad.setCadPackage(cad_package);
		cad.setCadTech(cad_tech);
		cad.setCadVersionData(cad_version_data);
		// Order
		Short isHaoDuoYi = Short
				.parseShort(request.getParameter("is_haoduoyi"));// 取得是否为好多衣属性
		Order order = new Order();
		order.setReorder((short) 0);
		order.setEmployeeId(employeeId);
		order.setCustomerId(customerId);
		order.setOrderState(orderState);
		order.setOrderTime(orderTime);
		order.setCustomerName(customerName);
		order.setCustomerCompany(customerCompany);
		order.setCustomerCompanyFax(customerCompanyFax);
		order.setCustomerPhone1(customerPhone1);
		order.setCustomerPhone2(customerPhone2);
		order.setCustomerCompanyAddress(customerCompanyAddress);
		order.setStyleName(styleName);
		order.setFabricType(fabricType);
		order.setClothesType(clothesType);
		order.setStyleSex(styleSex);
		order.setStyleSeason(styleSeason);
		order.setSpecialProcess(specialProcess);
		order.setOtherRequirements(otherRequirements);
		order.setReferenceUrl(referenceUrl);
		order.setSampleAmount(sample_amount);
		order.setAskAmount(askAmount);
		order.setAskProducePeriod(askProducePeriod);
		order.setAskDeliverDate(askDeliverDate);
		order.setAskCodeNumber(askCodeNumber);
		order.setHasPostedSampleClothes(hasPostedSampleClothes);
		order.setIsNeedSampleClothes(isNeedSampleClothes);
		order.setOrderSource(orderSource);
		order.setIsHaoDuoYi(isHaoDuoYi);
		String haoduoyi = request.getParameter("is_haoduoyi");
		Short ishaoduoyi = Short.parseShort(haoduoyi);

		if (order.getIsHaoDuoYi() == 1) {
			// 如果是好多衣客户
			order.setOrderSource("好多衣");
		}

		boolean isSuccess = false; 
		//如果是客户下单
		if (Constants.USER_ROLE_CUSTOMER.equals(account.getUserRole())) {
			int marketStaffId = Integer.parseInt(request
					.getParameter("marketStaffId"));
			//未选定市场专员
			if (marketStaffId == -1) {
				order.setOrderState("TODO");
				order.setEmployeeId(-1);
				
			}
			else {
				//设定市场专员
				order.setEmployeeId(marketStaffId);
			}
			isSuccess = marketService.addOrderCustomerSubmit(order, fabrics, accessorys,
					logistics, produces, sample_produces, versions, cad,
					request);
		} else {
			isSuccess =marketService.addOrderSubmit(order, fabrics, accessorys, logistics,
					produces, sample_produces, versions, cad, request);
			// 给客户邮箱发送订单信息
			marketService.sendOrderInfoViaEmail(order, customer);
			// 给客户手机发送订单信息
			marketService.sendOrderInfoViaPhone(order, customer);
		}
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
		jsonUtil.sendJson(response, model);

	}

	@RequestMapping(value = "/market/mobile_addMoreOrderList.do")
	public void addMoreOrderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String cid = request.getParameter("cid");
		String result = request.getParameter("result");
		String notify = "";
		if (result != null) {
			notify = "该订单为好多衣客户所下订单，暂时无法进行翻单！";
			request.setAttribute(Constants.JSON_NOTIFY, notify);
		}
		List<Map<String, Object>> list = marketService
				.getAddMoreOrderList(Integer.parseInt(cid));
		list = ListUtil.reserveList(list);
		model.put("list", list);
		model.addAttribute(Constants.JSON_NOTIFY, notify);
		model.addAttribute("taskName", "下翻单");
		model.addAttribute("url", "/market/mobile_addMoreOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_addMoreOrderListSearch.do");
		model.addAttribute("cid", cid);
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_addMoreOrderListSearch.do")
	public void addMoreOrderListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		String cid = request.getParameter("cid");
		List<Map<String, Object>> list = marketService
				.getSearchAddMoreOrderList(ordernumber, customername,
						stylename, startdate, enddate, employeeIds);
		list = ListUtil.reserveList(list);
		List<Map<String, Object>> resultlist = new ArrayList();
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> model1 = list.get(i);
			Order order = (Order) model1.get("order");
			if (order.getCustomerId() == Integer.parseInt(cid)) {
				resultlist.add(model1);
			}
		}
		model.put("list", resultlist);
		model.addAttribute("taskName", "下翻单");
		model.addAttribute("url", "/market/mobile_addMoreOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_addMoreOrderListSearch.do");
		model.addAttribute("cid", cid);
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_addMoreOrderDetail.do")
	// @Transactional(rollbackFor = Exception.class)
	public void addMoreOrderDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String cid = request.getParameter("cid");
		String s_id = request.getParameter("orderId");
		int id = Integer.parseInt(s_id);
		Map<String, Object> orderModel = marketService
				.getAddMoreOrderDetail(id);
		boolean isSuccess = false; 
		if (orderModel == null) {
			model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
			model.addAttribute(Constants.JSON_NOTIFY, "该订单未签订过大货合同，无法进行翻单！");
			// 若无法进行翻单，返回翻单列表
			jsonUtil.sendJson(response, model);
		}
		model.addAttribute("orderModel", orderModel);
		model.addAttribute("initId", id);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		model.addAttribute(Constants.JSON_EMPLOYEE_NAME, account.getNickName());
		jsonUtil.sendJson(response, model);

	}

	@RequestMapping(value = "/market/mobile_addMoreOrderSubmit.do")
	// @Transactional(rollbackFor = Exception.class)
	public void addMoreOrderSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		// 订单数据
		Integer customerId = Integer.parseInt(request
				.getParameter("customerId"));
		Customer customer = customerService.findByCustomerId(customerId);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Integer employeeId = account.getUserId();
		String orderState = "A";
		Timestamp orderTime = new Timestamp(new Date().getTime());
		String customerName = customer.getCustomerName();
		String customerCompany = customer.getCompanyName();
		String customerCompanyFax = customer.getCompanyFax();
		String customerPhone1 = customer.getContactPhone1();
		String customerPhone2 = customer.getContactPhone2();
		String customerCompanyAddress = customer.getCompanyAddress();
		String styleName = request.getParameter("style_name");
		String fabricType = request.getParameter("fabric_type");
		String clothesType = request.getParameter("clothes_type");
		String styleSex = request.getParameter("style_sex");
		String styleSeason = request.getParameter("style_season");
		String specialProcess = StringUtils.join(
				request.getParameterValues("special_process"), "|");
		String otherRequirements = StringUtils.join(
				request.getParameterValues("other_requirements"), "|");
		String referenceUrl = request.getParameter("reference_url");
		Integer askAmount = Integer
				.parseInt(request.getParameter("ask_amount"));
		String askProducePeriod = request.getParameter("ask_produce_period");
		String ask_deliver_date = request.getParameter("ask_deliver_date");
		Timestamp askDeliverDate = getAskDeliverDateTime(ask_deliver_date);
		String askCodeNumber = request.getParameter("ask_code_number");
		Short hasPostedSampleClothes = Short.parseShort(request
				.getParameter("has_posted_sample_clothes"));
		Short isNeedSampleClothes = Short.parseShort(request
				.getParameter("is_need_sample_clothes"));
		String orderSource = request.getParameter("order_source");
		String is_haoduoyi = request.getParameter("ishaoduoyi");
		Short ishaoduoyi = Short.parseShort(is_haoduoyi);// 是否为好多衣客户
		// 面料数据
		String fabric_names = request.getParameter("fabric_name");
		String fabric_amounts = request.getParameter("fabric_amount");
		String fabric_name[] = fabric_names.split(",");
		String fabric_amount[] = fabric_amounts.split(",");
		List<Fabric> fabrics = new ArrayList<Fabric>();
		for (int i = 0; i < fabric_name.length; i++) {
			if (fabric_name[i].equals(""))
				continue;
			fabrics.add(new Fabric(0, fabric_name[i], fabric_amount[i]));
		}

		// 辅料数据
		String accessory_names = request.getParameter("accessory_name");
		String accessory_querys = request.getParameter("accessory_query");
		String accessory_name[] = accessory_names.split(",");
		String accessory_query[] = accessory_querys.split(",");
		List<Accessory> accessorys = new ArrayList<Accessory>();
		for (int i = 0; i < accessory_name.length; i++) {
			if (accessory_name[i].equals(""))
				continue;
			accessorys.add(new Accessory(0, accessory_name[i],
					accessory_query[i]));
		}

		// 大货加工要求
		String produce_colors = request.getParameter("produce_color");
		String produce_xss = request.getParameter("produce_xs");
		String produce_ss = request.getParameter("produce_s");
		String produce_ms = request.getParameter("produce_m");
		String produce_ls = request.getParameter("produce_l");
		String produce_xls = request.getParameter("produce_xl");
		String produce_xxls = request.getParameter("produce_xxl");
		String produce_js = request.getParameter("produce_j");
		String produce_color[] = produce_colors.split(",");
		String produce_xs[] = produce_xss.split(",");
		String produce_s[] = produce_ss.split(",");
		String produce_m[] = produce_ms.split(",");
		String produce_l[] = produce_ls.split(",");
		String produce_xl[] = produce_xls.split(",");
		String produce_xxl[] = produce_xxls.split(",");
		String produce_j[] = produce_js.split(",");
		List<Produce> produces = new ArrayList<Produce>();
		for (int i = 0; i < produce_color.length; i++) {
			if (produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(produce_color[i]);
			p.setOid(0);

			int l = Integer.parseInt(produce_l[i]);
			int m = Integer.parseInt(produce_m[i]);
			int s = Integer.parseInt(produce_s[i]);
			int xs = Integer.parseInt(produce_xs[i]);
			int xl = Integer.parseInt(produce_xl[i]);
			int xxl = Integer.parseInt(produce_xxl[i]);
			int j = Integer.parseInt(produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setProduceAmount(l + m + s + xs + xl + xxl + j);
			p.setType(Produce.TYPE_PRODUCE);
			produces.add(p);
		}

		// 版型数据
		String version_sizes = request.getParameter("version_size");
		String version_centerBackLengths = request
				.getParameter("version_centerBackLength");
		String version_busts = request.getParameter("version_bust");
		String version_waistLines = request.getParameter("version_waistLine");
		String version_shoulders = request.getParameter("version_shoulder");
		String version_buttocks = request.getParameter("version_buttock");
		String version_hems = request.getParameter("version_hem");
		String version_trouserss = request.getParameter("version_trousers");
		String version_skirts = request.getParameter("version_skirt");
		String version_sleevess = request.getParameter("version_sleeves");
		String version_size[] = version_sizes.split(",");
		String version_centerBackLength[] = version_centerBackLengths
				.split(",");
		String version_bust[] = version_busts.split(",");
		String version_waistLine[] = version_waistLines.split(",");
		String version_shoulder[] = version_shoulders.split(",");
		String version_buttock[] = version_buttocks.split(",");
		String version_hem[] = version_hems.split(",");
		String version_trousers[] = version_trouserss.split(",");
		String version_skirt[] = version_skirts.split(",");
		String version_sleeves[] = version_sleevess.split(",");
		List<VersionData> versions = new ArrayList<VersionData>();
		for (int i = 0; i < version_size.length; i++) {
			if (version_size[i].equals(""))
				continue;
			versions.add(new VersionData(0, version_size[i],
					version_centerBackLength[i], version_bust[i],
					version_waistLine[i], version_shoulder[i],
					version_buttock[i], version_hem[i], version_trousers[i],
					version_skirt[i], version_sleeves[i]));
		}

		// 物流数据
		Logistics logistics = new Logistics();
		if (hasPostedSampleClothes == 1) {
			String in_post_sample_clothes_time = request
					.getParameter("in_post_sample_clothes_time");
			String in_post_sample_clothes_type = request
					.getParameter("in_post_sample_clothes_type");
			String in_post_sample_clothes_number = request
					.getParameter("in_post_sample_clothes_number");
			logistics
					.setInPostSampleClothesTime(getTime(in_post_sample_clothes_time));
			logistics.setInPostSampleClothesType(in_post_sample_clothes_type);
			logistics
					.setInPostSampleClothesNumber(in_post_sample_clothes_number);
		}
		// if (isNeedSampleClothes == 1) {

		String sample_clothes_name = request
				.getParameter("sample_clothes_name");
		String sample_clothes_phone = request
				.getParameter("sample_clothes_phone");
		String sample_clothes_address = request
				.getParameter("sample_clothes_address");
		String sample_clothes_remark = request
				.getParameter("sample_clothes_remark");

		logistics.setSampleClothesName(sample_clothes_name);
		logistics.setSampleClothesPhone(sample_clothes_phone);
		logistics.setSampleClothesAddress(sample_clothes_address);
		logistics.setSampleClothesRemark(sample_clothes_remark);
		// }

		// CAD
		DesignCad cad = new DesignCad();
		cad.setOrderId(0);
		cad.setCadVersion((short) 1);
		String cad_fabric = request.getParameter("cadFabric");
		String cad_box = request.getParameter("cadBox");
		String cad_package = request.getParameter("cadPackage");
		String cad_version_data = request.getParameter("cadVersionData");
		String cad_tech = request.getParameter("cadTech");
		String cad_other = request.getParameter("cadOther");
		cad.setCadBox(cad_box);
		cad.setCadFabric(cad_fabric);
		cad.setCadOther(cad_other);
		cad.setCadPackage(cad_package);
		cad.setCadTech(cad_tech);
		cad.setCadVersionData(cad_version_data);
		// Order
		Order order = new Order();
		order.setReorder((short) 1);
		order.setEmployeeId(employeeId);
		order.setCustomerId(customerId);
		order.setOrderState(orderState);
		order.setOrderTime(orderTime);
		order.setCustomerName(customerName);
		order.setCustomerCompany(customerCompany);
		order.setCustomerCompanyFax(customerCompanyFax);
		order.setCustomerPhone1(customerPhone1);
		order.setCustomerPhone2(customerPhone2);
		order.setCustomerCompanyAddress(customerCompanyAddress);
		order.setStyleName(styleName);
		order.setFabricType(fabricType);
		order.setClothesType(clothesType);
		order.setStyleSex(styleSex);
		order.setStyleSeason(styleSeason);
		order.setSpecialProcess(specialProcess);
		order.setOtherRequirements(otherRequirements);
		order.setReferenceUrl(referenceUrl);
		order.setAskAmount(askAmount);
		order.setSampleAmount(0);
		order.setAskProducePeriod(askProducePeriod);
		order.setAskDeliverDate(askDeliverDate);
		order.setAskCodeNumber(askCodeNumber);
		order.setHasPostedSampleClothes(hasPostedSampleClothes);
		order.setIsNeedSampleClothes(isNeedSampleClothes);
		order.setOrderSource(orderSource);
		order.setIsHaoDuoYi(ishaoduoyi);

		//如果是客户下单
		if ("CUSTOMER".equals(account.getUserRole())) {
			int marketStaffId = Integer.parseInt(request
					.getParameter("marketStaffId"));
		    //未选定市场专员
			if (marketStaffId == -1) {
				order.setOrderState("TODO");
				order.setEmployeeId(-1);;
						
			}
			else {
				//设定市场专员
				order.setEmployeeId(marketStaffId);
			}
			marketService.addMoreCustomerOrderSubmit(order, fabrics, accessorys, logistics, produces, versions, cad, request);
		}
		else{
			marketService.addMoreOrderSubmit(order, fabrics, accessorys, logistics,
				produces, versions, cad, request);
		}
		// 给客户邮箱发送订单信息
		marketService.sendOrderInfoViaEmail(order, customer);
		// 给客户手机发送订单信息
		marketService.sendOrderInfoViaPhone(order, customer);
		model.addAttribute(Constants.JSON_IS_SUCCESS, true);
		jsonUtil.sendJson(response, model);
	}
	
	//认领客户新单
	@RequestMapping(value="/market/mobile_claimCustomerOrderList.do", method = RequestMethod.GET)
	public void claimCustomerOrderList(HttpServletRequest request, HttpServletResponse response, ModelMap model){
		List<Map<String, Object>> list = marketService.getOrdersTodo();
		model.addAttribute("list", list);
		model.addAttribute("taskName", "客户新单列表");
		model.addAttribute("url", "/market/mobile_claimCustomerOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_claimCustomerOrderSearch.do");
		jsonUtil.sendJson(response, model);
	}
	
	@RequestMapping(value="/market/mobile_claimCustomerOrderDetail.do", method = RequestMethod.GET)
	public void claimCustomerOrderDetail(HttpServletRequest request, HttpServletResponse response, ModelMap model){
		String orderId = request.getParameter("orderId");
		int id = Integer.parseInt(orderId);
		Map<String, Object> orderInfo = marketService.getOrderDetail(id);
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}
	
	@RequestMapping(value="/market/mobile_claimCustomerOrderSearch.do", method=RequestMethod.POST)
	public void claimCustomerOrderSearch(HttpServletRequest request, HttpServletResponse response, ModelMap model){
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		List<Map<String, Object>> list = marketService.getSearchTodoOrderList(ordernumber, customername, stylename, startdate, enddate);

		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/market/mobile_claimCustomerOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_claimCustomerOrderSearch.do");
		jsonUtil.sendJson(response, model);
	}
	
	@RequestMapping(value="/market/mobile_claimCustomerOrderSubmit.do", method=RequestMethod.POST)
	public void claimCustomerOrderSubmit(HttpServletRequest request, HttpServletResponse response, ModelMap model){
		int orderId =  Integer.valueOf(request.getParameter("orderId"));
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		boolean isSuccess = marketService.claimCustomerOrder(orderId, account.getUserId());
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
	}

	// test precondition
	@RequestMapping(value = "/market/mobile_precondition.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public String precondition(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		marketService
				.testPrecondition("CAIGOUZHUGUAN", "Purchasing_accounting");
		marketService.testPrecondition("SHEJIZHUGUAN", "design_accounting");
		marketService.testPrecondition("SHENGCHANZHUGUAN",
				"business_accounting");

		return null;
	}

	@Autowired
	private MarketService marketService;

	// 专员修改报价
	@RequestMapping(value = "market/mobile_mobile_modifyQuoteSubmit.do", method = RequestMethod.POST)
	public void modifyQuoteSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String s_profit = request.getParameter("profitPerPiece");
		String innerPrice = request.getParameter("inner_price");
		String outerPrice = request.getParameter("outer_price");
		String s_single = request.getParameter("single_cost");
		String orderId = request.getParameter("order_id");
		String s_taskId = request.getParameter("taskId");
		String s_processId = request.getParameter("processId");
		float profit = 0;
		float inner = 0;
		float outer = 0;
		float single = 0;
		if (!s_profit.equals(""))
			profit = Float.parseFloat(s_profit);
		if (!innerPrice.equals(""))
			inner = Float.parseFloat(innerPrice);
		if (!outerPrice.equals(""))
			outer = Float.parseFloat(outerPrice);
		if (!s_single.equals(""))
			single = Float.parseFloat(s_single);
		int id = Integer.parseInt(orderId);
		String taskId = s_taskId;
		String processId = s_processId;
		Quote quote = quoteService.findByOrderId(orderId);
		quote.setSingleCost(single);
		quote.setProfitPerPiece(profit);
		quote.setInnerPrice(inner);
		quote.setOuterPrice(outer);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		marketService.modifyQuoteSubmit(quote, id, taskId, processId,
				account.getUserId());
		model.addAttribute(Constants.JSON_IS_SUCCESS, true);
		jsonUtil.sendJson(response, model);
	}

	// 专员修改报价
	@RequestMapping(value = "market/mobile_modifyQuoteDetail.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void modifyQuoteDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		String orderId = request.getParameter("orderId");
		// String s_processId=request.getParameter("pid");
		int id = Integer.parseInt(orderId);
		// String processId=String.parseString(s_processId);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Map<String, Object> orderInfo = marketService.getModifyQuoteDetail(id,
				account.getUserId());
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	// 专员修改报价列表
	@RequestMapping(value = "market/mobile_mobile_modifyQuoteList.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void modifyQuoteList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> tasks = marketService
				.getModifyQuoteList(account.getUserId());
		tasks = ListUtil.reserveList(tasks);
		model.put("list", tasks);
		model.addAttribute("taskName", "修改报价");
		model.addAttribute("url", "/market/mobile_modifyQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_modifyQuoteListSearch.do");
		jsonUtil.sendJson(response, model);
	}

	// 专员修改报价列表搜索
	@RequestMapping(value = "market/mobile_modifyQuoteListSearch.do")
	// @Transactional(rollbackFor = Exception.class)
	public void modifyQuoteListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> tasks = marketService
				.getSearchModifyQuoteList(account.getUserId(), ordernumber,
						customername, stylename, startdate, enddate,
						employeeIds);

		tasks = ListUtil.reserveList(tasks);
		model.put("list", tasks);
		model.addAttribute("taskName", "修改报价订单搜索");
		model.addAttribute("url", "/market/mobile_modifyQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_modifyQuoteListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	// 专员修改加工单列表
	@RequestMapping(value = "market/mobile_modifyProductList.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void modifyProductList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> tasks = marketService
				.getModifyProductList(account.getUserId());
		tasks = ListUtil.reserveList(tasks);
		model.put("list", tasks);
		model.addAttribute("taskName", "修改合同加工单");
		model.addAttribute("url", "/market/mobile_modifyProductDetail.do");
		model.addAttribute("searchurl", "/market/mobile_modifyProductListSearch.do");

		jsonUtil.sendJson(response, model);
	}

	// 专员修改加工单列表搜索
	@RequestMapping(value = "market/mobile_modifyProductListSearch.do")
	public void modifyProductListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性

		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> tasks = marketService
				.getSearchModifyProductList(account.getUserId(), ordernumber,
						customername, stylename, startdate, enddate,
						employeeIds);

		tasks = ListUtil.reserveList(tasks);
		model.put("list", tasks);
		model.addAttribute("taskName", "修改合同加工单搜索");
		model.addAttribute("url", "/market/mobile_modifyProductDetail.do");
		model.addAttribute("searchurl", "/market/mobile_modifyProductListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	// 专员修改加工单详情
	@RequestMapping(value = "market/mobile_modifyProductDetail.do", method = RequestMethod.GET)
	public void modifyProductDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String orderId = request.getParameter("orderId");
		int id = Integer.parseInt(orderId);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Map<String, Object> oi = marketService.getModifyProductDetail(id,
				account.getUserId());
		model.addAttribute("orderInfo", oi);
		jsonUtil.sendJson(response, model);
	}

	// 专员修改加工单
	@RequestMapping(value = "market/mobile_modifyProductSubmit.do", method = RequestMethod.POST)
	public void modifyProductSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String s_orderId_request = (String) request.getParameter("orderId");
		int orderId_request = Integer.parseInt(s_orderId_request);
		String s_taskId = request.getParameter("taskId");
		String taskId = s_taskId;
		String s_processId = request.getParameter("processId");
		String processId = s_processId;
		boolean editworksheetok = Boolean.parseBoolean(request
				.getParameter("tof"));
		// 大货加工要求
		String produce_colors = request.getParameter("produce_color");
		String produce_xss = request.getParameter("produce_xs");
		String produce_ss = request.getParameter("produce_s");
		String produce_ms = request.getParameter("produce_m");
		String produce_ls = request.getParameter("produce_l");
		String produce_xls = request.getParameter("produce_xl");
		String produce_xxls = request.getParameter("produce_xxl");
		String produce_js = request.getParameter("produce_j");
		String produce_color[] = produce_colors.split(",");
		String produce_xs[] = produce_xss.split(",");
		String produce_s[] = produce_ss.split(",");
		String produce_m[] = produce_ms.split(",");
		String produce_l[] = produce_ls.split(",");
		String produce_xl[] = produce_xls.split(",");
		String produce_xxl[] = produce_xxls.split(",");
		String produce_j[] = produce_js.split(",");
		List<Produce> produces = new ArrayList<Produce>();
		for (int i = 0; i < produce_color.length; i++) {
			if (produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(produce_color[i]);
			p.setOid(0);
			int l = Integer.parseInt(produce_l[i]);
			int m = Integer.parseInt(produce_m[i]);
			int s = Integer.parseInt(produce_s[i]);
			int xs = Integer.parseInt(produce_xs[i]);
			int xl = Integer.parseInt(produce_xl[i]);
			int xxl = Integer.parseInt(produce_xxl[i]);
			int j = Integer.parseInt(produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setProduceAmount(l + m + s + xs + xl + xxl + j);
			p.setType(Produce.TYPE_PRODUCE);
			produces.add(p);
		}

		boolean isSuccess = marketService.modifyProductSubmit(account.getUserId() + "",
				orderId_request, taskId, processId, editworksheetok, produces);
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
		jsonUtil.sendJson(response, model);
	}

	// 专员合并报价
	@RequestMapping(value = "market/mobile_mergeQuoteSubmit.do", method = RequestMethod.POST)
	// @Transactional(rollbackFor = Exception.class)
	public void mergeQuoteSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String s_profit = request.getParameter("profitPerPiece");
		String innerPrice = request.getParameter("inner_price");
		String outerPrice = request.getParameter("outer_price");
		String s_single = request.getParameter("single_cost");
		String orderId = request.getParameter("order_id");
		String s_taskId = request.getParameter("taskId");
		String s_processId = request.getParameter("processId");
		float profit = 0;
		float inner = 0;
		float outer = 0;
		float single = 0;
		if (!s_profit.equals(""))
			profit = Float.parseFloat(s_profit);
		if (!innerPrice.equals(""))
			inner = Float.parseFloat(innerPrice);
		if (!outerPrice.equals(""))
			outer = Float.parseFloat(outerPrice);
		if (!s_single.equals(""))
			single = Float.parseFloat(s_single);
		int id = Integer.parseInt(orderId);
		String taskId = s_taskId;
		String processId = s_processId;

		Quote quote = quoteService.findByOrderId(orderId);
		quote.setProfitPerPiece(profit);
		quote.setInnerPrice(inner);
		quote.setOuterPrice(outer);
		quote.setSingleCost(single);
		
		marketService.mergeQuoteSubmit(account.getUserId(), quote, id, taskId,
				processId);
		model.addAttribute(Constants.JSON_IS_SUCCESS, true);
		jsonUtil.sendJson(response, model);
	}

	// 专员合并报价信息
	@RequestMapping(value = "market/mobile_mergeQuoteDetail.do")
	// @Transactional(rollbackFor = Exception.class)
	public void mergeQuoteDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String s_id = request.getParameter("orderId");
		int id = Integer.parseInt(s_id);
		Map<String, Object> orderModel = marketService.getMergeQuoteDetail(
				account.getUserId(), id);
		model.addAttribute("orderInfo", orderModel);
		model.addAttribute("merge_w", true);
		String verifyQuoteComment = marketService.getComment(
				orderModel.get("taskId"), MarketServiceImpl.VERIFY_QUOTE_COMMENT);
		model.addAttribute("verifyQuoteComment", verifyQuoteComment);
		jsonUtil.sendJson(response, model);
	}

	// 专员合并报价List
	@RequestMapping(value = "market/mobile_mergeQuoteList.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void mergeQuoteList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = marketService
				.getMergeQuoteList(account.getUserId());
		list = ListUtil.reserveList(list);
		/*
		 * if (list.size() == 0) {
		 * jbpmTest.completeComputeCost(account.getAccountId() + ""); list =
		 * marketService.getMergeQuoteList(account.getAccountId()); }
		 */

		model.put("list", list);
		model.addAttribute("taskName", "合并报价");
		model.addAttribute("url", "/market/mobile_mergeQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_mergeQuoteListSearch.do");
		jsonUtil.sendJson(response, model);
	}

	// 专员合并报价ListSearch
	@RequestMapping(value = "market/mobile_mergeQuoteListSearch.do")
	public void mergeQuoteListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		List<Map<String, Object>> list = marketService.getSearchMergeQuoteList(
				account.getUserId(), ordernumber, customername, stylename,
				startdate, enddate, employeeIds);
		list = ListUtil.reserveList(list);
		/*
		 * if (list.size() == 0) {
		 * jbpmTest.completeComputeCost(account.getAccountId() + ""); list =
		 * marketService.getMergeQuoteList(account.getAccountId()); }
		 */

		model.put("list", list);
		model.addAttribute("taskName", "合并报价订单查找");
		model.addAttribute("url", "/market/mobile_mergeQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_mergeQuoteListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	// 主管审核报价
	@RequestMapping(value = "market/mobile_verifyQuoteSubmit.do", method = RequestMethod.POST)
	// @Transactional(rollbackFor = Exception.class)
	public void verifyQuoteSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		/*
		 * Map params = new HashMap(); params.put("page", 1);
		 * params.put("number_per_page", 100); List list =
		 * customerService.listCustomer(params);
		 * model.addAttribute("customer_list", list.get(0));
		 */
		String s_single = request.getParameter("single_cost");
		String s_profit = request.getParameter("profitPerPiece");
		String innerPrice = request.getParameter("inner_price");
		String outerPrice = request.getParameter("outer_price");
		String orderId = request.getParameter("order_id");
		String s_taskId = request.getParameter("taskId");
		String s_processId = request.getParameter("processId");

		float profit = 0;
		float inner = 0;
		float outer = 0;
		float single = 0;
		if (!s_profit.equals(""))
			profit = Float.parseFloat(s_profit);
		if (!innerPrice.equals(""))
			inner = Float.parseFloat(innerPrice);
		if (!outerPrice.equals(""))
			outer = Float.parseFloat(outerPrice);
		if (!s_single.equals(""))
			single = Float.parseFloat(s_single);
		int id = Integer.parseInt(orderId);
		String taskId = s_taskId;
		String processId = s_processId;
		Quote quote = quoteService.findByOrderId(orderId);
		quote.setSingleCost(single);
		quote.setProfitPerPiece(profit);
		quote.setInnerPrice(inner);
		quote.setOuterPrice(outer);
		boolean result = Boolean.parseBoolean(request
				.getParameter("verifyQuoteSuccessVal"));
		String comment = request.getParameter("suggestion");
		// marketService.verifyQuoteSubmit(quote, id, taskId, processId);
		marketService.verifyQuoteSubmit(quote, id, taskId, processId, result,
				comment);
		model.addAttribute(Constants.JSON_IS_SUCCESS, true);
		jsonUtil.sendJson(response, model);
	}

	// 主管审核报价detail
	@RequestMapping(value = "market/mobile_verifyQuoteDetail.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void verifyQuoteDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String s_id = request.getParameter("orderId");
		int id = Integer.parseInt(s_id);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Map<String, Object> orderModel = marketService.getVerifyQuoteDetail(
				account.getUserId(), id);
		model.addAttribute("orderInfo", orderModel);
		jsonUtil.sendJson(response, model);
	}

	// 主管审核报价List
	@RequestMapping(value = "market/mobile_verifyQuoteList.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void verifyQuoteList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = marketService
				.getVerifyQuoteList(account.getUserId());
		list = ListUtil.reserveList(list);
		model.put("list", list);
		model.addAttribute("taskName", "审核报价");
		model.addAttribute("url", "/market/mobile_verifyQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_verifyQuoteListSearch.do");

		jsonUtil.sendJson(response, model);

	}

	// 主管审核报价List搜索
	@RequestMapping(value = "market/mobile_verifyQuoteListSearch.do")
	public void verifyQuoteListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		List<Map<String, Object>> list = marketService
				.getSearchVerifyQuoteList(account.getUserId(), ordernumber,
						customername, stylename, startdate, enddate,
						employeeIds);
		list = ListUtil.reserveList(list);
		model.put("list", list);
		model.addAttribute("taskName", "审核报价订单查询");
		model.addAttribute("url", "/market/mobile_verifyQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_verifyQuoteListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);

	}
	

	// 修改询单的列表
	@RequestMapping(value = "market/mobile_modifyOrderList.do", method = RequestMethod.GET)
	public void modifyOrderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> orderModelList = marketService
				.getModifyOrderList(account.getUserId());
		orderModelList = ListUtil.reserveList(orderModelList);
		/*
		 * if (orderModelList.size() == 0) {
		 * jbpmTest.completeVerify(account.getUserId() + "", false);
		 * orderModelList = marketService.getModifyOrderList(account
		 * .getUserId()); }
		 */
		model.put("list", orderModelList);
		model.addAttribute("taskName", "修改询单");
		model.addAttribute("url", "/market/mobile_modifyOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_modifyOrderListSearch.do");
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_modifyOrderListSearch.do")
	public void modifyOrderSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}
		List<Map<String, Object>> list = null;
		Integer userId = account.getUserId();
		list = marketService.getSearchModifyOrderList(userId, ordernumber,
				customername, stylename, startdate, enddate, employeeIds);
		list = ListUtil.reserveList(list);
		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "修改询单");
		model.addAttribute("url", "/market/mobile_modifyOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_modifyOrderListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	// 询单的修改界面
	@RequestMapping(value = "market/mobile_modifyOrderDetail.do")
	// @Transactional(rollbackFor = Exception.class)
	public void modifyOrderDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		String s_id = request.getParameter("orderId");
		int id = Integer.parseInt(s_id);
		// 修改
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Map<String, Object> orderModel = marketService.getModifyOrderDetail(
				account.getUserId(), id);
		model.addAttribute("orderModel", orderModel);
		Object taskId = orderModel.get("taskId");
		String purchaseComment = marketService.getComment(taskId,
				BuyServiceImpl.RESULT_PURCHASE_COMMENT);
		String designComment = marketService.getComment(taskId,
				DesignServiceImpl.RESULT_DESIGN_COMMENT);
		String produceComment = marketService.getComment(taskId,
				ProduceServiceImpl.RESULT_PRODUCE_COMMENT);

		model.addAttribute("purchaseComment", purchaseComment);
		model.addAttribute("designComment", designComment);
		model.addAttribute("produceComment", produceComment);
		jsonUtil.sendJson(response, model);
	}

	// 询单的修改界面
	@RequestMapping(value = "market/mobile_modifyOrderSubmit.do", method = RequestMethod.POST)
	public void modifyOrderSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		
		String s_id = request.getParameter("id");
		String s_task_id = request.getParameter("task_id");
		int id = Integer.parseInt(s_id);
		String task_id = s_task_id;
		// 保存修改该的order数据，accessory，fabric，logistics
		// 订单数据

		String orderState = "A";
		String styleName = request.getParameter("style_name");
		String fabricType = request.getParameter("fabric_type");
		String clothesType = request.getParameter("clothes_type");
		String styleSex = request.getParameter("style_sex");
		String styleSeason = request.getParameter("style_season");
		String specialProcess = StringUtils.join(
				request.getParameterValues("special_process"), "|");
		String otherRequirements = StringUtils.join(
				request.getParameterValues("other_requirements"), "|");
		String referenceUrl = request.getParameter("reference_url");
		Calendar calendar = Calendar.getInstance();

		Integer askAmount = Integer
				.parseInt(request.getParameter("ask_amount"));
		String askProducePeriod = request.getParameter("ask_produce_period");
		Timestamp askDeliverDate = getAskDeliverDateTime(request
				.getParameter("ask_deliver_date"));
		String askCodeNumber = request.getParameter("ask_code_number");
		Short hasPostedSampleClothes = Short.parseShort(request
				.getParameter("has_posted_sample_clothes"));
		Short isNeedSampleClothes = Short.parseShort(request
				.getParameter("is_need_sample_clothes"));
		String orderSource = request.getParameter("order_source");

		// 面料数据
		String fabric_names = request.getParameter("fabric_name");
		String fabric_amounts = request.getParameter("fabric_amount");
		String fabric_name[] = fabric_names.split(",");
		String fabric_amount[] = fabric_amounts.split(",");
		List<Fabric> fabrics = new ArrayList<Fabric>();
		for (int i = 0; i < fabric_name.length; i++) {
			fabrics.add(new Fabric(0, fabric_name[i], fabric_amount[i]));
		}

		// 辅料数据
		String accessory_names = request.getParameter("accessory_name");
		String accessory_querys = request.getParameter("accessory_query");
		String accessory_name[] = accessory_names.split(",");
		String accessory_query[] = accessory_querys.split(",");
		List<Accessory> accessorys = new ArrayList<Accessory>();
		for (int i = 0; i < accessory_name.length; i++) {
			accessorys.add(new Accessory(0, accessory_name[i],
					accessory_query[i]));
		}

		// 大货加工要求
		String produce_colors = request.getParameter("produce_color");
		String produce_xss = request.getParameter("produce_xs");
		String produce_ss = request.getParameter("produce_s");
		String produce_ms = request.getParameter("produce_m");
		String produce_ls = request.getParameter("produce_l");
		String produce_xls = request.getParameter("produce_xl");
		String produce_xxls = request.getParameter("produce_xxl");
		String produce_js = request.getParameter("produce_j");
		String produce_color[] = produce_colors.split(",");
		String produce_xs[] = produce_xss.split(",");
		String produce_s[] = produce_ss.split(",");
		String produce_m[] = produce_ms.split(",");
		String produce_l[] = produce_ls.split(",");
		String produce_xl[] = produce_xls.split(",");
		String produce_xxl[] = produce_xxls.split(",");
		String produce_j[] = produce_js.split(",");
		List<Produce> produces = new ArrayList<Produce>();
		for (int i = 0; i < produce_color.length; i++) {
			if (produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(produce_color[i]);
			p.setOid(0);
			int l = Integer.parseInt(produce_l[i]);
			int m = Integer.parseInt(produce_m[i]);
			int s = Integer.parseInt(produce_s[i]);
			int xs = Integer.parseInt(produce_xs[i]);
			int xl = Integer.parseInt(produce_xl[i]);
			int xxl = Integer.parseInt(produce_xxl[i]);
			int j = Integer.parseInt(produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setProduceAmount(l + m + s + xs + xl + xxl + j);
			p.setType(Produce.TYPE_PRODUCE);
			produces.add(p);
		}

		// 样衣加工要求
		String sample_produce_colors = request
				.getParameter("sample_produce_color");
		String sample_produce_xss = request.getParameter("sample_produce_xs");
		String sample_produce_ss = request.getParameter("sample_produce_s");
		String sample_produce_ms = request.getParameter("sample_produce_m");
		String sample_produce_ls = request.getParameter("sample_produce_l");
		String sample_produce_xls = request.getParameter("sample_produce_xl");
		String sample_produce_xxls = request.getParameter("sample_produce_xxl");
		String sample_produce_js = request.getParameter("sample_produce_j");
		String sample_produce_color[] = sample_produce_colors.split(",");
		String sample_produce_xs[] = sample_produce_xss.split(",");
		String sample_produce_s[] = sample_produce_ss.split(",");
		String sample_produce_m[] = sample_produce_ms.split(",");
		String sample_produce_l[] = sample_produce_ls.split(",");
		String sample_produce_xl[] = sample_produce_xls.split(",");
		String sample_produce_xxl[] = sample_produce_xxls.split(",");
		String sample_produce_j[] = sample_produce_js.split(",");
		List<Produce> sample_produces = new ArrayList<Produce>();
		int sample_amount = 0;
		for (int i = 0; i < sample_produce_color.length; i++) {
			if (sample_produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(sample_produce_color[i]);
			p.setOid(0);
			int l = Integer.parseInt(sample_produce_l[i]);
			int m = Integer.parseInt(sample_produce_m[i]);
			int s = Integer.parseInt(sample_produce_s[i]);
			int xs = Integer.parseInt(sample_produce_xs[i]);
			int xl = Integer.parseInt(sample_produce_xl[i]);
			int xxl = Integer.parseInt(sample_produce_xxl[i]);
			int j = Integer.parseInt(sample_produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setType(Produce.TYPE_SAMPLE_PRODUCE);
			int temp = l + m + s + xs + xl + xxl + j;
			p.setProduceAmount(temp);
			sample_amount += temp;
			sample_produces.add(p);
		}

		// 版型数据
		String version_sizes = request.getParameter("version_size");
		String version_centerBackLengths = request
				.getParameter("version_centerBackLength");
		String version_busts = request.getParameter("version_bust");
		String version_waistLines = request.getParameter("version_waistLine");
		String version_shoulders = request.getParameter("version_shoulder");
		String version_buttocks = request.getParameter("version_buttock");
		String version_hems = request.getParameter("version_hem");
		String version_trouserss = request.getParameter("version_trousers");
		String version_skirts = request.getParameter("version_skirt");
		String version_sleevess = request.getParameter("version_sleeves");
		String version_size[] = version_sizes.split(",");
		String version_centerBackLength[] = version_centerBackLengths
				.split(",");
		String version_bust[] = version_busts.split(",");
		String version_waistLine[] = version_waistLines.split(",");
		String version_shoulder[] = version_shoulders.split(",");
		String version_buttock[] = version_buttocks.split(",");
		String version_hem[] = version_hems.split(",");
		String version_trousers[] = version_trouserss.split(",");
		String version_skirt[] = version_skirts.split(",");
		String version_sleeves[] = version_sleevess.split(",");
		List<VersionData> versions = new ArrayList<VersionData>();
		for (int i = 0; i < version_size.length; i++) {
			if (version_size[i].equals(""))
				continue;
			versions.add(new VersionData(0, version_size[i],
					version_centerBackLength[i], version_bust[i],
					version_waistLine[i], version_shoulder[i],
					version_buttock[i], version_hem[i], version_trousers[i],
					version_skirt[i], version_sleeves[i]));
		}

		// 物流数据
		Logistics logistics = logisticsService.findByOrderId(s_id);
		if (hasPostedSampleClothes == 1) {
			String in_post_sample_clothes_time = request
					.getParameter("in_post_sample_clothes_time");
			String in_post_sample_clothes_type = request
					.getParameter("in_post_sample_clothes_type");
			String in_post_sample_clothes_number = request
					.getParameter("in_post_sample_clothes_number");

			logistics
					.setInPostSampleClothesTime(getAskDeliverDateTime(in_post_sample_clothes_time));
			logistics.setInPostSampleClothesType(in_post_sample_clothes_type);
			logistics
					.setInPostSampleClothesNumber(in_post_sample_clothes_number);
		}
		// if (isNeedSampleClothes == 1) {
		// String sample_clothes_time = request
		// .getParameter("sample_clothes_time");
		// String sample_clothes_type = request
		// .getParameter("sample_clothes_type");
		// String sample_clothes_number = request
		// .getParameter("sample_clothes_number");
		String sample_clothes_name = request
				.getParameter("sample_clothes_name");
		String sample_clothes_phone = request
				.getParameter("sample_clothes_phone");
		String sample_clothes_address = request
				.getParameter("sample_clothes_address");
		String sample_clothes_remark = request
				.getParameter("sample_clothes_remark");

		// logistics.setSampleClothesTime(getTime(sample_clothes_time));
		// logistics.setSampleClothesType(sample_clothes_type);
		// logistics.setSampleClothesNumber(sample_clothes_number);
		logistics.setSampleClothesName(sample_clothes_name);
		logistics.setSampleClothesPhone(sample_clothes_phone);
		logistics.setSampleClothesAddress(sample_clothes_address);
		logistics.setSampleClothesRemark(sample_clothes_remark);
		// }

		// CAD
		DesignCad cad = cadService.findByOrderId(s_id);
		// cad.setCadVersion((short) 1);
		String cad_fabric = request.getParameter("cadFabric");
		String cad_box = request.getParameter("cadBox");
		String cad_package = request.getParameter("cadPackage");
		String cad_version_data = request.getParameter("cadVersionData");
		String cad_tech = request.getParameter("cadTech");
		String cad_other = request.getParameter("cadOther");
		cad.setCadBox(cad_box);
		cad.setCadFabric(cad_fabric);
		cad.setCadOther(cad_other);
		cad.setCadPackage(cad_package);
		cad.setCadTech(cad_tech);
		cad.setCadVersionData(cad_version_data);

		// Order
		Order order = orderService.findByOrderId(s_id);
		// order.setEmployeeId(employeeId);
		// order.setCustomerId(customerId);
		order.setOrderState(orderState);
		// order.setCustomerName(customerName);
		// order.setCustomerCompany(customerCompany);
		// order.setCustomerCompanyFax(customerCompanyFax);
		// order.setCustomerPhone1(customerPhone1);
		// order.setCustomerPhone2(customerPhone2);
		// order.setCustomerCompanyAddress(customerCompanyAddress);
		order.setStyleName(styleName);
		order.setFabricType(fabricType);
		order.setClothesType(clothesType);
		order.setStyleSex(styleSex);
		order.setStyleSeason(styleSeason);
		order.setSpecialProcess(specialProcess);
		order.setOtherRequirements(otherRequirements);
		order.setReferenceUrl(referenceUrl);
		order.setAskAmount(askAmount);
		order.setSampleAmount(sample_amount);
		order.setAskProducePeriod(askProducePeriod);
		order.setAskDeliverDate(askDeliverDate);
		order.setAskCodeNumber(askCodeNumber);
		// order.setHasPostedSampleClothes(hasPostedSampleClothes);
		// order.setIsNeedSampleClothes(isNeedSampleClothes);
		order.setOrderSource(orderSource);

		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		if (!multipartRequest.getFile("sample_clothes_picture").isEmpty()) {
			/**
			 * 应采用绝对路径
			 */
			/*
			 * File file = new File(order.getSampleClothesPicture());
			 * if(file.exists()){ file.delete(); } MultipartFile mfile =
			 * multipartRequest.getFile("sample_clothes_picture"); String
			 * filename = mfile.getOriginalFilename(); String url =
			 * MarketServiceImpl.UPLOAD_DIR_SAMPLE + order.getOrderId(); String
			 * fileid = "sample_clothes_picture";
			 * FileOperateUtil.Upload(request, url, null, fileid);
			 * ImageUtil.createThumbnail(url, url+File.separator);
			 * order.setSampleClothesThumbnailPicture(url + "/" +
			 * "thumbnail.png"); url = url + "/" + filename;
			 * order.setSampleClothesPicture(url);
			 */

			String curPath = request.getSession().getServletContext()
					.getRealPath("/");
			String fatherPath = new File(curPath).getParent();

			String filedir = fatherPath + order.getSampleClothesPicture();
			File file = new File(filedir);
			if (file.exists()) {
				file.delete();
			}
			file = new File(fatherPath
					+ order.getSampleClothesThumbnailPicture());
			if (file.exists()) {
				file.delete();
			}
			System.out.println(filedir.substring(0, filedir.lastIndexOf("/"))
					+ "----------");
			FileOperateUtil.Upload(request,
					filedir.substring(0, filedir.lastIndexOf("/")), "1",
					"sample_clothes_picture");
			ImageUtil.createThumbnail(
					filedir.substring(0, filedir.lastIndexOf("/")),
					filedir.substring(0, filedir.lastIndexOf("/") + 1));

		}
		if (!multipartRequest.getFile("reference_picture").isEmpty()) {
			/**
			 * 应采用绝对路径
			 */
			/*
			 * File file = new File(order.getReferencePicture());
			 * if(file.exists()){ file.delete(); } MultipartFile mfile =
			 * multipartRequest.getFile("reference_picture"); String filename =
			 * mfile.getOriginalFilename(); String url =
			 * MarketServiceImpl.UPLOAD_DIR_REFERENCE + order.getOrderId();
			 * String fileid = "reference_picture";
			 * FileOperateUtil.Upload(request, url, null, fileid); url = url +
			 * "/" + filename; order.setReferencePicture(url);
			 */
			String curPath = request.getSession().getServletContext()
					.getRealPath("/");
			String fatherPath = new File(curPath).getParent();

			String filedir = fatherPath + order.getReferencePicture();
			File file = new File(filedir);
			if (file.exists()) {
				file.delete();
			}
			FileOperateUtil.Upload(request,
					filedir.substring(0, filedir.lastIndexOf("/")), "1",
					"reference_picture");
		}

		boolean editok = request.getParameter("editok").equals("true") ? true
				: false;
		marketService.modifyOrderSubmit(order, fabrics, accessorys, logistics,
				produces, sample_produces, versions, cad, editok, task_id,
				account.getUserId());
		model.addAttribute(Constants.JSON_IS_SUCCESS, true);
		jsonUtil.sendJson(response, model);
	}

	public static Timestamp getTime(String time) {
		if (time.equals(""))
			return null;
		Date outDate = DateUtil.parse(time, DateUtil.haveSecondFormat);
		return new Timestamp(outDate.getTime());
	}

	public static Timestamp getAskDeliverDateTime(String time) {
		if (time.equals(""))
			return null;
		Date outDate = DateUtil.parse(time, DateUtil.newFormat);
		return new Timestamp(outDate.getTime());
	}

	@RequestMapping(value = "/market/mobile_confirmQuoteList.do")
	public void confirmQuoteList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = marketService
				.getConfirmQuoteList(account.getUserId() + "");
		list = ListUtil.reserveList(list);
		model.put("list", list);
		model.addAttribute("taskName", "确认报价");
		model.addAttribute("url", "/market/mobile_confirmQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_confirmQuoteListSearch.do");

		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_confirmQuoteListSearch.do")
	public void confirmQuoteListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}
		List<Map<String, Object>> list = marketService
				.getSearchConfirmQuoteList(account.getUserId() + "",
						ordernumber, customername, stylename, startdate,
						enddate, employeeIds);
		list = ListUtil.reserveList(list);
		model.put("list", list);		model.addAttribute("taskName", "确认报价搜索");
		model.addAttribute("url", "/market/mobile_confirmQuoteDetail.do");
		model.addAttribute("searchurl", "/market/mobile_confirmQuoteListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_confirmQuoteDetail.do")
	public void confirmQuoteDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String s_id = request.getParameter("orderId");
		int id = Integer.parseInt(s_id);
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		Map<String, Object> orderModel = marketService.getConfirmQuoteDetail(
				account.getUserId(), id);
		model.addAttribute("orderInfo", orderModel);
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_confirmQuoteSubmit.do")
	public void confirmQuoteSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String result = request.getParameter("result");
		String taskId = request.getParameter("taskId");
		String orderId = request.getParameter("orderId");
		String moneyremark = request.getParameter("moneyremark");// 金额备注
		String url = "";
		// result为0，表示上传样衣制作金
		if (result.equals("0")) {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			MultipartFile file = multipartRequest
					.getFile("confirmSampleMoneyFile");
			String filename = "";
			if (file != null) {
				filename = file.getOriginalFilename();
			}

			// 将图片保存在和项目根目录同级的文件夹upload下
			String curPath = request.getSession().getServletContext()
					.getRealPath("/");// 获取当前路径
			String fatherPath = new File(curPath).getParent();// 当前路径的上级目录
			String relativePath = File.separator + UPLOAD_DIR + File.separator
					+ "confirmSampleMoneyFile" + File.separator + orderId;
			String filedir = fatherPath + relativePath;// 最终要保存的路径

			String fileid = "confirmSampleMoneyFile";
			FileOperateUtil.Upload(request, filedir, null, fileid);

			url = CONFIRM_SAMPLEMONEY_URL + orderId + "/" + filename;// 保存在数据库里的相对路径
		}
		String actorId = account.getUserId() + "";
		// marketService.confirmQuoteSubmit(actorId,
		// String.parseString(taskId),result);
		boolean isSuccess = marketService.confirmQuoteSubmit(actorId, taskId,
				Integer.parseInt(orderId), result, url, moneyremark);

		// 1=修改报价，2=取消订单
		if (result.equals("1")) {
			model.addAttribute("result", "modifyQuote");
		} else {
			model.addAttribute("result", "cancelOrder");
		}
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
		jsonUtil.sendJson(response, model);
	}

	// ============================确认合同加工单===========================

	/**
	 * 确认合同加工单跳转链接
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "market/mobile_confirmProduceOrderList.do", method = RequestMethod.GET)
	// @Transactional(rollbackFor = Exception.class)
	public void confirmProduceOrderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String actorId = account.getUserId() + "";
		List<Map<String, Object>> orderList = marketService
				.getConfirmProductList(actorId);
		orderList = ListUtil.reserveList(orderList);
		// if (orderList.size() == 0) {
		// jbpmTest.completeProduceConfirm("1", true);
		// orderList = marketService.getConfirmProductList(actorId);
		// }
		model.put("list", orderList);
		model.addAttribute("taskName", "确认合同加工单");
		model.addAttribute("url", "/market/mobile_confirmProduceOrderDetail.do");
		model.addAttribute("searchurl",
				"/market/mobile_confirmProduceOrderListSearch.do");

		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "market/mobile_confirmProduceOrderListSearch.do")
	// @Transactional(rollbackFor = Exception.class)
	public void confirmProduceOrderListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String actorId = account.getUserId() + "";
		List<Map<String, Object>> orderList = marketService
				.getSearchConfirmProductList(actorId, ordernumber,
						customername, stylename, startdate, enddate,
						employeeIds);

		// if (orderList.size() == 0) {
		// jbpmTest.completeProduceConfirm("1", true);
		// orderList = marketService.getConfirmProductList(actorId);
		// }
		orderList = ListUtil.reserveList(orderList);
		model.put("list", orderList);
		model.addAttribute("taskName", "确认合同加工单");
		model.addAttribute("url", "/market/mobile_confirmProduceOrderDetail.do");
		model.addAttribute("searchurl",
				"/market/mobile_confirmProduceOrderListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	/**
	 * 确认合同加工单
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "market/mobile_confirmProduceOrderSubmit.do", method = RequestMethod.POST)
	public void confirmProduceOrderSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String s_orderId_request = (String) request.getParameter("orderId");
		int orderId_request = Integer.parseInt(s_orderId_request);
		String s_taskId = request.getParameter("taskId");
		String taskId = s_taskId;
		String s_processId = request.getParameter("processId");
		String processId = s_processId;
		String tof = (String) request.getParameter("tof");
		boolean comfirmworksheet = Boolean.parseBoolean(request
				.getParameter("tof"));
		// 大货加工要求
		String produce_colors = request.getParameter("produce_color");
		String produce_xss = request.getParameter("produce_xs");
		String produce_ss = request.getParameter("produce_s");
		String produce_ms = request.getParameter("produce_m");
		String produce_ls = request.getParameter("produce_l");
		String produce_xls = request.getParameter("produce_xl");
		String produce_xxls = request.getParameter("produce_xxl");
		String produce_js = request.getParameter("produce_j");
		String produce_color[] = produce_colors.split(",");
		String produce_xs[] = produce_xss.split(",");
		String produce_s[] = produce_ss.split(",");
		String produce_m[] = produce_ms.split(",");
		String produce_l[] = produce_ls.split(",");
		String produce_xl[] = produce_xls.split(",");
		String produce_xxl[] = produce_xxls.split(",");
		String produce_j[] = produce_js.split(",");
		List<Produce> produces = new ArrayList<Produce>();
		for (int i = 0; i < produce_color.length; i++) {
			if (produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(produce_color[i]);
			p.setOid(0);
			int l = Integer.parseInt(produce_l[i]);
			int m = Integer.parseInt(produce_m[i]);
			int s = Integer.parseInt(produce_s[i]);
			int xs = Integer.parseInt(produce_xs[i]);
			int xl = Integer.parseInt(produce_xl[i]);
			int xxl = Integer.parseInt(produce_xxl[i]);
			int j = Integer.parseInt(produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setProduceAmount(l + m + s + xs + xl + xxl + j);
			p.setType(Produce.TYPE_PRODUCE);
			produces.add(p);
		}
		// 当用户确认加工单的时候，才选择上传合同和定金截图，否则不上传
		if (comfirmworksheet) {
			String discount = request.getParameter("discount");
			String total = request.getParameter("totalmoney");
			String orderId = request.getParameter("orderId");
			String moneyremark = request.getParameter("moneyremark");
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			MultipartFile contractFile = multipartRequest
					.getFile("contractFile");
			MultipartFile confirmDepositFile = multipartRequest
					.getFile("confirmDepositFile");
			String contractFileName = contractFile.getOriginalFilename();
			String confirmDepositFileName = confirmDepositFile
					.getOriginalFilename();

			// 将图片保存在和项目根目录同级的文件夹upload下
			String curPath = request.getSession().getServletContext()
					.getRealPath("/");// 获取当前路径
			String fatherPath = new File(curPath).getParent();// 当前路径的上级目录
			String contractRelativePath = File.separator + UPLOAD_DIR
					+ File.separator + "contract" + File.separator + orderId;
			String depositRelativePath = File.separator + UPLOAD_DIR
					+ File.separator + "confirmDepositFile" + File.separator
					+ orderId;
			String contractFileDir = fatherPath + contractRelativePath;// 最终合同保存的路径
			String depositFileDir = fatherPath + depositRelativePath;// 最终首定金要保存的路径

			String fileid = "contractFile";
			String confirmDepositFileId = "confirmDepositFile";
			FileOperateUtil.Upload(request, contractFileDir, null, fileid);
			FileOperateUtil.Upload(request, depositFileDir, null,
					confirmDepositFileId);

			String contractFileUrl = CONTRACT_URL + orderId + "/"
					+ contractFileName;
			String confirmDepositFileUrl = CONFIRM_DEPOSIT_URL + orderId + "/"
					+ confirmDepositFileName;

			String actorId = account.getUserId() + "";
			// 上传合同，上传首定金收据，一般是截图，
			marketService.signContractSubmit(actorId, s_taskId,
					Integer.parseInt(orderId), Double.parseDouble(discount),
					Double.parseDouble(total), contractFileUrl,
					confirmDepositFileUrl, moneyremark);

		}

		boolean isSuccess = marketService.confirmProduceOrderSubmit(account.getUserId() + "",
				orderId_request, taskId, processId, comfirmworksheet, produces);
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
		jsonUtil.sendJson(response, model);
	}

	/**
	 * 确认合同加工单详情
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "market/mobile_confirmProduceOrderDetail.do")
	public void confirmProduceOrderDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String s_orderId_request = (String) request.getParameter("orderId");
		int id = Integer.parseInt(s_orderId_request);
		// String s_taskId = request.getParameter("taskId");
		// String taskId = String.parseString(s_taskId);
		Map<String, Object> orderInfo = marketService.getConfirmProductDetail(
				account.getUserId(), id);
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	/**
	 * 取消订单
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "market/mobile_cancelProduct.do", method = RequestMethod.POST)
	public void cancelSample(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String s_orderId_request = (String) request.getParameter("id");
		int orderId_request = Integer.parseInt(s_orderId_request);
		String s_taskId = request.getParameter("task_id");
		String taskId = s_taskId;
		String s_processId = request.getParameter("process_id");
		String processId = s_processId;
		boolean comfirmworksheet = false;
		boolean isSuccess = marketService.confirmProduceOrderSubmit(account.getUserId() + "",
				orderId_request, taskId, processId, comfirmworksheet, null);
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
		jsonUtil.sendJson(response, model);
	}

	// ========================市场专员催尾款===============================

	@RequestMapping(value = "/market/mobile_getPushRestOrderList.do")
	public void getPushRestOrderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = marketService
				.getPushRestOrderList(account.getUserId() + "");
		list = ListUtil.reserveList(list);
		model.put("list", list);
		model.addAttribute("taskName", "催尾款");
		model.addAttribute("url", "/market/mobile_getPushRestOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_getPushRestOrderListSearch.do");
		jsonUtil.sendJson(response, model);
	}

	// ========================市场专员催尾款搜索===============================
	@RequestMapping(value = "/market/mobile_getPushRestOrderListSearch.do")
	public void getPushRestOrderListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = marketService
				.getSearchPushRestOrderList(account.getUserId() + "",
						ordernumber, customername, stylename, startdate,
						enddate, employeeIds);
		list = ListUtil.reserveList(list);
		model.put("list", list);
		model.addAttribute("taskName", "催尾款搜索");
		model.addAttribute("url", "/market/mobile_getPushRestOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_getPushRestOrderListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	// @RequestMapping(value = "/market/mobile_getPushRestOrderListSearch.do")
	// //@Transactional(rollbackFor = Exception.class)
	// public String getPushRestOrderListSearch(HttpServletRequest request,
	// HttpServletResponse response, ModelMap model) {
	// Account account = (Account) request.getSession().getAttribute(
	// "cur_user");
	// List<Map<String, Object>> list = marketService
	// .getPushRestOrderList(account.getUserId()+"");
	// model.put("list", list);
	// model.addAttribute("taskName", "催尾款");
	// model.addAttribute("url", "/market/mobile_getPushRestOrderDetail.do");
	// model.addAttribute("searchurl", "/market/mobile_getPushRestOrderListSearch.do");
	// return "/market/mobile_getPushRestOrderList";
	// }

	@RequestMapping(value = "/market/mobile_getPushRestOrderDetail.do")
	public void getPushRestOrderDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String orderId = request.getParameter("orderId");
		Map<String, Object> orderInfo = marketService.getPushRestOrderDetail(
				account.getUserId() + "", Integer.parseInt(orderId));
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	// 上传接收尾金截图
	@RequestMapping(value = "/market/mobile_confirmFinalPaymentFileSubmit.do")
	public void confirmFinalPaymentFileSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String orderId = request.getParameter("orderId");
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		MultipartFile confirmFinalPaymentFile = multipartRequest
				.getFile("confirmFinalPaymentFile");
		String confirmFinalPaymentFileName = confirmFinalPaymentFile
				.getOriginalFilename();

		// 将图片保存在和项目根目录同级的文件夹upload下
		String curPath = request.getSession().getServletContext()
				.getRealPath("/");// 获取当前路径
		String fatherPath = new File(curPath).getParent();// 当前路径的上级目录
		String relativePath = File.separator + UPLOAD_DIR + File.separator
				+ "confirmFinalPaymentFile" + File.separator + orderId;
		String filedir = fatherPath + relativePath;// 最终要保存的路径

		String confirmFinalPaymentFileId = "confirmFinalPaymentFile";

		FileOperateUtil.Upload(request, filedir, null,
				confirmFinalPaymentFileId);

		String confirmFinalPaymentFileUrl = CONFIRM_FINALPAYMENT_URL + orderId
				+ "/" + confirmFinalPaymentFileName;
		String moneyremark = request.getParameter("moneyremark");

		// 上传尾定金收据，一般是截图
		marketService.signConfirmFinalPaymentFileSubmit(
				Integer.parseInt(orderId), confirmFinalPaymentFileUrl,
				moneyremark);
		Map<String, Object> orderInfo = marketService.getPushRestOrderDetail(
				account.getUserId() + "", Integer.parseInt(orderId));
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);

	}

	@RequestMapping(value = "/market/mobile_getPushRestOrderSubmit.do")
	// @Transactional(rollbackFor = Exception.class)
	public void getPushRestOrderSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String orderId_string = request.getParameter("orderId");
		String taskId_string = request.getParameter("taskId");
		String taskId = taskId_string;
		String moneyremark = request.getParameter("moneyremark");
		// result=0，催尾款失败；result=1，确认收到尾款
		boolean result = request.getParameter("result").equals("1");
		if (result) {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			MultipartFile confirmFinalPaymentFile = multipartRequest
					.getFile("confirmFinalPaymentFile");
			String confirmFinalPaymentFileName = confirmFinalPaymentFile
					.getOriginalFilename();

			// 将图片保存在和项目根目录同级的文件夹upload下
			String curPath = request.getSession().getServletContext()
					.getRealPath("/");// 获取当前路径
			String fatherPath = new File(curPath).getParent();// 当前路径的上级目录
			String relativePath = File.separator + UPLOAD_DIR + File.separator
					+ "confirmFinalPaymentFile" + File.separator
					+ orderId_string;
			String filedir = fatherPath + relativePath;// 最终要保存的路径

			String confirmFinalPaymentFileId = "confirmFinalPaymentFile";
			FileOperateUtil.Upload(request, filedir, null,
					confirmFinalPaymentFileId);

			String confirmFinalPaymentFileUrl = CONFIRM_FINALPAYMENT_URL
					+ orderId_string + "/" + confirmFinalPaymentFileName;

			// 上传尾定金收据，一般是截图，
			marketService.signConfirmFinalPaymentFileSubmit(
					Integer.parseInt(orderId_string),
					confirmFinalPaymentFileUrl, moneyremark);
		}
		String actorId = account.getUserId() + "";

		boolean isSuccess = marketService.getPushRestOrderSubmit(actorId, taskId, result,
				orderId_string);
		model.addAttribute(Constants.JSON_IS_SUCCESS, isSuccess);
		jsonUtil.sendJson(response, model);
	}

	// ========================签订合同============================
	@RequestMapping(value = "/market/mobile_signContractList.do")
	public void signContractList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = marketService
				.getSignContractList(account.getUserId() + "");
		list = ListUtil.reserveList(list);
		/*
		 * if (list.size() == 0) {
		 * jbpmTest.completeProduceConfirm(account.getUserId() + "", true);
		 * marketService.getSignContractList(account.getUserId() + ""); }
		 */
		model.put("list", list);
		model.addAttribute("taskName", "签订合同");
		model.addAttribute("url", "/market/mobile_signContractDetail.do");
		model.addAttribute("searchurl", "/market/mobile_signContractListSearch.do");

		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_signContractListSearch.do")
	public void signContractListSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}

		List<Map<String, Object>> list = marketService
				.getSearchSignContractList(account.getUserId() + "",
						ordernumber, customername, stylename, startdate,
						enddate, employeeIds);
		list = ListUtil.reserveList(list);
		/*
		 * if (list.size() == 0) {
		 * jbpmTest.completeProduceConfirm(account.getUserId() + "", true);
		 * marketService.getSignContractList(account.getUserId() + ""); }
		 */
		model.put("list", list);
		model.addAttribute("taskName", "签订合同");
		model.addAttribute("url", "/market/mobile_signContractDetail.do");
		model.addAttribute("searchurl", "/market/mobile_signContractListSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_signContractDetail.do")
	// @Transactional(rollbackFor = Exception.class)
	public void signContractDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String orderId = request.getParameter("orderId");
		Map<String, Object> orderInfo = marketService.getSignContractDetail(
				account.getUserId() + "", Integer.parseInt(orderId));
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "market/mobile_signContractSubmit.do", method = RequestMethod.POST)
	public void signContractSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String discount = request.getParameter("discount");
		String total = request.getParameter("totalmoney");
		String orderId = request.getParameter("orderId");
		String taskId = request.getParameter("taskId");
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		MultipartFile contractFile = multipartRequest.getFile("contractFile");
		MultipartFile confirmDepositFile = multipartRequest
				.getFile("confirmDepositFile");
		String contractFileName = contractFile.getOriginalFilename();
		String confirmDepositFileName = confirmDepositFile
				.getOriginalFilename();

		// 将图片保存在和项目根目录同级的文件夹upload_new下
		String curPath = request.getSession().getServletContext()
				.getRealPath("/");// 获取当前路径
		String fatherPath = new File(curPath).getParent();// 当前路径的上级目录
		String contractRelativePath = File.separator + UPLOAD_DIR
				+ File.separator + "contract" + File.separator + orderId;
		String depositRelativePath = File.separator + UPLOAD_DIR
				+ File.separator + "confirmDepositFile" + File.separator
				+ orderId;
		String contractFileDir = fatherPath + contractRelativePath;// 最终合同保存的路径
		String depositFileDir = fatherPath + depositRelativePath;// 最终首定金要保存的路径

		String fileid = "contractFile";
		String confirmDepositFileId = "confirmDepositFile";
		FileOperateUtil.Upload(request, contractFileDir, null, fileid);
		FileOperateUtil.Upload(request, depositFileDir, null,
				confirmDepositFileId);

		String contractFileUrl = CONTRACT_URL + orderId + "/"
				+ contractFileName;
		String confirmDepositFileUrl = CONFIRM_DEPOSIT_URL + orderId + "/"
				+ confirmDepositFileName;

		String actorId = account.getUserId() + "";
		// 上传合同，上传首定金收据，一般是截图，
		marketService.signContractSubmit(actorId, taskId,
				Integer.parseInt(orderId), Double.parseDouble(discount),
				Double.parseDouble(total), contractFileUrl,
				confirmDepositFileUrl, "");// "" hcj

		Map<String, Object> orderInfo = marketService.getConfirmProductDetail(
				account.getUserId(), Integer.parseInt(orderId));
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/order/mobile_orderList.do")
	public void orderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {

		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = null;

		// 客户和市场专员只能看到与自己相关的订单
		if ("CUSTOMER".equals(account.getUserRole())
				|| "marketStaff".equals(account.getUserRole())) {
			list = marketService.getOrders(account.getUserRole(),
					account.getUserId());
		} else {
			list = marketService.getOrders();
		}
		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/order/mobile_orderDetail.do");
		model.addAttribute("searchurl", "/order/mobile_orderSearch.do");
		jsonUtil.sendJson(response, model);
	}

	

	@RequestMapping(value = "/order/mobile_orderSearch.do")
	public void orderSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}

		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		/*
		 * int j=0; SearchInfo info = null;
		 */
		SearchInfo searchInfo = getSearchInfo(ordernumber, customername,
				stylename, startdate, enddate, employeename);
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}
		/*
		 * SearchInfo info = new SearchInfo();
		 * info.setCustomername(customername);
		 * info.setEmployeename(employeename); info.setEnddate(enddate);
		 * info.setOrdernumber(ordernumber); info.setStartdate(startdate);
		 * info.setStylename(stylename);
		 * System.out.println("--------"+info.getStylename());
		 */
		List<Map<String, Object>> list = marketService.getSearchOrderList(
				ordernumber, customername, stylename, startdate, enddate,
				employeeIds, account.getUserRole(), account.getUserId());

		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表查找");
		model.addAttribute("url", "/order/mobile_orderDetail.do");
		model.addAttribute("searchurl", "/order/mobile_orderSearch.do");
		model.addAttribute("info", searchInfo);// 将查询条件传回页面 hcj

		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/order/mobile_orderDetail.do")
	public void orderDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String orderStateName = "";
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		ArrayList<String> orderProcessStateNames = marketService
				.getProcessStateName(orderId);
		if (orderProcessStateNames.size() > 0) {
			for (int i = 0; i < orderProcessStateNames.size(); i++) {
				if (!orderProcessStateNames.get(i).equals("Gateway")) {
					if (i > 0)
						orderStateName += " | ";
					orderStateName += orderProcessStateNames.get(i);
				}
			}
		}
		if (orderStateName != "") {
			request.setAttribute("orderStateMessage", "当前任务是：" + orderStateName);
		}
		Map<String, Object> orderInfo = marketService.getOrderDetail(orderId);
		model.addAttribute("orderInfo", orderInfo);
		model.addAttribute("role", account.getUserRole());
		jsonUtil.sendJson(response, model);
	}

	// 正在进行中的订单 就是这个
	@RequestMapping(value = "/order/mobile_orderListDoing.do")
	public void orderListDoing(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = null;

		// 客户和市场专员只能看到与自己相关的进行中的订单
		if ("CUSTOMER".equals(account.getUserRole())
				|| "marketStaff".equals(account.getUserRole())) {
			list = marketService.getOrdersDoing(account.getUserRole(),
					account.getUserId());
		} else {
			list = marketService.getOrdersDoing();
		}
		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}

		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/order/mobile_orderDetail.do");
		model.addAttribute("searchurl", "/order/mobile_orderListDoingSearch.do");
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/order/mobile_orderListDoingSearch.do")
	public void orderListDoingSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		String orderProcessStateName = request
				.getParameter("orderProcessStateName");
		// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
		List<Employee> employees = employeeService
				.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}
		List<Map<String, Object>> list = marketService.getSearchOrdersDoing(
				ordernumber, orderProcessStateName, customername, stylename,
				startdate, enddate, employeeIds, account.getUserRole(),
				account.getUserId());

		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/order/mobile_orderDetail.do");
		model.addAttribute("searchurl", "/order/mobile_orderListDoingSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/order/mobile_orderListDone.do")
	public void orderListDone(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = null;

		// 客户和市场专员只能看到与自己相关的已经完成的订单
		if ("CUSTOMER".equals(account.getUserRole())
				|| "marketStaff".equals(account.getUserRole())) {
			list = marketService.getOrdersDone(account.getUserRole(),
					account.getUserId());
		} else {
			list = marketService.getOrdersDone();
		}
		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/order/mobile_orderDetail.do");
		model.addAttribute("searchurl", "/order/mobile_orderListDoneSearch.do");
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/order/mobile_orderListDoneSearch.do")
	public void orderListDoneSearch(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		// 获取当前登录用户
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}

		String ordernumber = request.getParameter("ordernumber");
		String customername = request.getParameter("customername");
		String stylename = request.getParameter("stylename");
		String employeename = request.getParameter("employeename");
		String startdate = request.getParameter("startdate");
		String enddate = request.getParameter("enddate");
		List<Employee> employees = new ArrayList<Employee>();
		employees = employeeService.getEmployeeByName(employeename);
		Integer[] employeeIds = new Integer[employees.size()];
		for (int i = 0; i < employeeIds.length; i++) {
			employeeIds[i] = employees.get(i).getEmployeeId();
		}
		List<Map<String, Object>> list = marketService.getSearchOrdersDone(
				ordernumber, customername, stylename, startdate, enddate,
				employeeIds, account.getUserRole(), account.getUserId());

		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/order/mobile_orderDetail.do");
		model.addAttribute("searchurl", "/order/mobile_orderListDoneSearch.do");
		model.addAttribute("info", new SearchInfo(ordernumber, customername,
				stylename, employeename, startdate, enddate));// 将查询条件传回页面 hcj
		jsonUtil.sendJson(response, model);
	}
		

	// 获取大货补货单信息
	@RequestMapping(value = "/market/mobile_printProcurementOrder.do")
	public void printProcurementOrder(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		Map<String, Object> orderInfo = buyService
				.getPrintProcurementOrderDetail(orderId, null);
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_printConfirmProcurementOrderHDY.do")
	public void printConfirmProcurementOrderHDY(HttpServletRequest request,
			HttpServletResponse response, ModelMap model)
			throws UnsupportedEncodingException {
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		Map<String, Object> orderInfo = buyService
				.getPrintProcurementOrderDetail(orderId, null);
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	// 获取大货补货单信息 printConfirmProcurementOrder
	@RequestMapping(value = "/market/mobile_printConfirmProcurementOrder.do")
	public void printConfirmProcurementOrder(HttpServletRequest request,
			HttpServletResponse response, ModelMap model)
			throws UnsupportedEncodingException {
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		// 大货加工要求
		String produce_colors = new String(request
				.getParameter("produce_color").getBytes("ISO8859-1"), "UTF-8");
		String produce_xss = request.getParameter("produce_xs");
		String produce_ss = request.getParameter("produce_s");
		String produce_ms = request.getParameter("produce_m");
		String produce_ls = request.getParameter("produce_l");
		String produce_xls = request.getParameter("produce_xl");
		String produce_xxls = request.getParameter("produce_xxl");
		String produce_js = request.getParameter("produce_j");
		String produce_color[] = produce_colors.split(",");
		String produce_xs[] = produce_xss.split(",");
		String produce_s[] = produce_ss.split(",");
		String produce_m[] = produce_ms.split(",");
		String produce_l[] = produce_ls.split(",");
		String produce_xl[] = produce_xls.split(",");
		String produce_xxl[] = produce_xxls.split(",");
		String produce_j[] = produce_js.split(",");
		List<Produce> produces = new ArrayList<Produce>();
		for (int i = 0; i < produce_color.length; i++) {
			if (produce_color[i].equals(""))
				continue;
			Produce p = new Produce();
			p.setColor(produce_color[i]);
			p.setOid(0);
			int l = Integer.parseInt(produce_l[i]);
			int m = Integer.parseInt(produce_m[i]);
			int s = Integer.parseInt(produce_s[i]);
			int xs = Integer.parseInt(produce_xs[i]);
			int xl = Integer.parseInt(produce_xl[i]);
			int xxl = Integer.parseInt(produce_xxl[i]);
			int j = Integer.parseInt(produce_j[i]);
			p.setL(l);
			p.setM(m);
			p.setS(s);
			p.setXl(xl);
			p.setXs(xs);
			p.setXxl(xxl);
			p.setJ(j);
			p.setProduceAmount(l + m + s + xs + xl + xxl + j);
			p.setType(Produce.TYPE_PRODUCE);
			produces.add(p);
		}
		Map<String, Object> orderInfo = buyService
				.getPrintProcurementOrderDetail(orderId, produces);
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	// 获取样衣裁剪单信息
	@RequestMapping(value = "/market/mobile_printProcurementSampleOrder.do")
	public void printProcurementSampleOrder(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		Map<String, Object> orderInfo = buyService
				.getPrintProcurementOrderDetail(orderId, null);
		model.addAttribute("orderInfo", orderInfo);
		jsonUtil.sendJson(response, model);
	}

	public SearchInfo getSearchInfo(String ordernumber, String customername,
			String stylename, String startdate, String enddate,
			String employeename) {
		SearchInfo info = new SearchInfo();
		info.setCustomername(customername);
		info.setEmployeename(employeename);
		info.setEnddate(enddate);
		info.setOrdernumber(ordernumber);
		info.setStartdate(startdate);
		info.setStylename(stylename);

		return info;
	}

	@RequestMapping(value = "/market/mobile_applyForAlterMarketStaff.do")
	public void applyForAlterMarketStaff(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		List<Map<String, Object>> list = null;

		// 客户和市场专员只能看到与自己相关的订单
		if ("CUSTOMER".equals(account.getUserRole())
				|| "marketStaff".equals(account.getUserRole())) {
			list = marketService.getOrdersDoing(account.getUserRole(),
					account.getUserId());
		} else {
			list = marketService.getOrders();
		}
		//修改界面无专员和无进度问题
		for (Map<String, Object> a:list){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		model.addAttribute("list", list);
		model.addAttribute("taskName", "订单列表");
		model.addAttribute("url", "/market/mobile_showApplyForAlterMarketStaff.do");
		model.addAttribute("searchurl", "/order/mobile_orderSearch.do");
		jsonUtil.sendJson(response, model);
	}

	@RequestMapping(value = "/market/mobile_showApplyForAlterMarketStaff.do")
	public void showApplyForAlterMarketStaff(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		Account account = commonService.getCurAccount(request, response);
		if (account == null) {
			return;
		}
		String orderStateName = "";
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		ArrayList<String> orderProcessStateNames = marketService
				.getProcessStateName(orderId);
		if (orderProcessStateNames.size() > 0) {
			for (int i = 0; i < orderProcessStateNames.size(); i++) {
				if (!orderProcessStateNames.get(i).equals("Gateway")) {
					if (i > 0)
						orderStateName += " | ";
					orderStateName += orderProcessStateNames.get(i);
				}
			}
		}
		if (orderStateName != "") {
			request.setAttribute("orderStateMessage", "当前任务是：" + orderStateName);
		}
		Map<String, Object> orderInfo = marketService.getOrderDetail(orderId);
		model.addAttribute("orderInfo", orderInfo);
		model.addAttribute("role", account.getUserRole());
		jsonUtil.sendJson(response, model);
	}

	/**
	 * 申请更改市场专员
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/market/mobile_applyForAlterMarketStaffSubmit.do", method = RequestMethod.POST)
	public void applyForAlterMarketStaffSubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		String reason = request.getParameter("reason");
		//申请更换专员订单的执行流ID
		String orderProcessId = request.getParameter("orderProcessId");
		Integer orderId = Integer.parseInt(request.getParameter("orderId"));
		Integer employeeId = Integer.parseInt(request
				.getParameter("employeeId"));

		// 判断是否存在重复申请
		boolean existRepetition = false;
		List<Map<String, Object>> applyList = marketService
				.getAlterInfoByOrderId(orderId);
		for (Map<String, Object> applyInfo : applyList) {
			MarketstaffAlter marketstaffAlter = (MarketstaffAlter) applyInfo
					.get(MarketServiceImpl.ALTER_ALTERINFO);
			// 存在重复申请
			if (employeeId.equals(marketstaffAlter.getEmployeeId())
					&& MarketstaffAlter.STATE_TODO.equals(marketstaffAlter
							.getVerifyState())) {
				existRepetition = true;
				break;
			}
		}
		String json;
		// 若不存在则提交申请
		if (!existRepetition) {
			Date date = new Date();
			Timestamp applyTime = new Timestamp(date.getTime());

			MarketstaffAlter alterInfo = new MarketstaffAlter(employeeId,
					orderId, applyTime);
			alterInfo.setVerifyState(MarketstaffAlter.STATE_TODO);

			boolean result = marketService.applyForAlterMarketStaffSubmit(
					alterInfo, reason, orderProcessId);

			Map<String, Object> map = new HashMap<>();
			map.put("result", result);
			map.put("reason", reason);
			map.put("orderProcessId", orderProcessId);

			JSONObject jsonObject = JSONObject.fromObject(map);
			json = jsonObject.toString();
		} else {
			Map<String, Object> map = new HashMap<>();
			map.put("result", "existRepetition");
			JSONObject jsonObject = JSONObject.fromObject(map);
			json = jsonObject.toString();
		}
		response.setContentType("application/json");
		try {
			response.getWriter().write(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 市场专员获得当前已有的更换专员申请
	 * 
	 * @param request
	 * @param response
	 * @param model
	 */
	@RequestMapping(value = "/market/mobile_getAlterInfoByOrderId.do")
	public void getAlterInfoByOrderId(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		int orderId = Integer.parseInt(request.getParameter("orderId"));

		List<Map<String, Object>> alterInfoList = marketService
				.getAlterInfoByOrderId(orderId);
		Map<String, Object> map = new HashMap<>();
		map.put("alterInfoList", alterInfoList);

		JSONObject jsonObject = JSONObject.fromObject(map);
		String json = jsonObject.toString();

		response.setContentType("application/json");
		try {
			response.getWriter().write(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 主管审核申请
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 * @throws InterruptedException 
	 */
	@RequestMapping(value = "/market/mobile_verifyAlterSubmit.do", method = RequestMethod.POST)
	public void verifySubmit(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) throws InterruptedException {
		/*
		 * int taskId =Integer.parseInt(request.getParameter("taskId")); int
		 * processId = Integer.parseInt(request.getParameter("processId")); 待定
		 */
		int alterId = Integer.parseInt(request.getParameter("alter_id"));
		boolean result = Boolean.parseBoolean(request
				.getParameter("verifyAlterSuccessVal"));

		String verifyState = request.getParameter("verifyState");

		String suggestion = request.getParameter("suggestion");

		MarketstaffAlter Alter = marketService.getMarketStaffAlterById(alterId);
		Date date = new Date();
		Timestamp endTime = new Timestamp(date.getTime());

		Alter.setEndTime(endTime);

		Alter.setVerifyState(verifyState);

		// 是否同意 ，设置了下一个专员
		if (result) {
			int nextEmployeeId = Integer.parseInt(request
					.getParameter("nextEmployeeId"));
			Alter.setNextEmployeeId(nextEmployeeId);
		} else {
			Alter.setNextEmployeeId(Alter.getEmployeeId());
		}

		// marketService.verifyQuoteSubmit(Alter, suggestion);
		// marketService.verifyAlterSubmit(Alter, taskId,
		// processId,result,suggestion);

		String taskId = request.getParameter("taskId");
		String processId = request.getParameter("processId");
		
		marketService.verifyAlterSubmit(Alter, taskId, processId, result,
				suggestion);
		model.addAttribute("result", result);
		model.addAttribute(Constants.JSON_IS_SUCCESS, true);
		jsonUtil.sendJson(response, model);

	}

	/**
	 * 主管审核申请表详细
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */

	// 主管审核申请表detail
	@RequestMapping(value = "market/mobile_verifyAlterDetail.do", method = RequestMethod.GET)
	public void verifyAlterDetail(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		int orderId = Integer.parseInt(request.getParameter("orderId"));
		int alterId = Integer.parseInt(request.getParameter("alterId"));
		List<Employee> employeeList = employeeService.getAllManagerStaff();
		Map<String, Object> orderModel = marketService.getOrderDetail(orderId);
		Map<String, Object> alterModel = marketService.getAlterInfoByAlterId(alterId);
		model.addAttribute("orderInfo", orderModel);
		model.addAttribute("alterModel", alterModel);
		model.addAttribute("employeeList", employeeList);
		jsonUtil.sendJson(response, model);
	}

	// 主管审核申请表List
	@RequestMapping(value = "/market/mobile_verifyAlterList.do", method = RequestMethod.GET)
	public void verifyAlterList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		List<MarketstaffAlter> alterList = marketService.getAlltoDoAlter();
		List<Map<String, Object>> list = new ArrayList<>();

		for (MarketstaffAlter staffAlter : alterList) {

			Map<String, Object> alterInfo = new HashMap<String, Object>();
			Employee employee_next = null;
			Employee employee = employeeService.getEmployeeById(staffAlter
					.getEmployeeId());
			Map order = marketService.getOrderDetail(staffAlter.getOrderId());
			// StaffAlterInfo alterInfo= new
			// StaffAlterInfo(staffAlter,employee,employee_next);

			alterInfo.put("employee", employee);
			alterInfo.put("employeeNext", employee_next);
			alterInfo.put("Alter", staffAlter);
			alterInfo.put("orderInfo", order);
			list.add(alterInfo);

		}

		// AlterInfo_list (Alter,employee_next,employee);
		model.put("list", list);
		model.addAttribute("taskName", "审核申请");
		model.addAttribute("url", "/market/mobile_verifyAlterDetail.do");
		model.addAttribute("searchurl", "/market/mobile_verifyAlterListSearch.do");

		jsonUtil.sendJson(response, model);

	}
	//秘书部分配订单列表
	@RequestMapping(value = "/market/mobile_allocateOrderList.do", method = RequestMethod.GET)
	public void allocateOrderList(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		
		List<Map<String, Object>> orderModelList = marketService.getOrdersTodo();
		
		
		
		orderModelList = ListUtil.reserveList(orderModelList);
		//修改界面无专员和无进度问题
		for (Map<String, Object> a:orderModelList){
			
			Order o=(Order) a.get("order");
			if (o.getOrderState().equals("TODO")){			
				o.setOrderProcessStateName("未选定专员");
				Employee employee=new Employee();
				employee.setEmployeeName("无");
				a.put("order", o);
				a.put("employee", employee);
			
			}
			
		}
		/*
		 * if (orderModelList.size() == 0) {
		 * jbpmTest.completeVerify(account.getUserId() + "", false);
		 * orderModelList = marketService.getModifyOrderList(account
		 * .getUserId()); }
		 */
		model.put("list", orderModelList);
		model.addAttribute("taskName", "分配订单");
		model.addAttribute("url", "/market/mobile_allocateOrderDetail.do");
		model.addAttribute("searchurl", "/market/mobile_allocateOrderSearch.do");

		jsonUtil.sendJson(response, model);
	}
	// 秘书分配订单detail
		@RequestMapping(value = "/market/mobile_allocateOrderDetail.do", method = RequestMethod.GET)
		public void allocateOrderDetail(HttpServletRequest request,
				HttpServletResponse response, ModelMap model) {
			String s_id = request.getParameter("orderId");
			int id = Integer.parseInt(s_id);
			HttpSession session = request.getSession();
			Map<String, Object> orderModel = marketService.getOrderDetail(id);
			//修改界面无专员和无进度问题
			
				
				Order o=(Order) orderModel.get("order");
				if (o.getOrderState().equals("TODO")){			
					o.setOrderProcessStateName("未选定专员");
					Employee employee=new Employee();
					employee.setEmployeeName("无");
					orderModel.put("order", o);
					orderModel.put("employee", employee);
				
				}
				
			
			List<Employee> employeeList = employeeService.getAllManagerStaff();
			model.addAttribute("orderInfo", orderModel);
			model.addAttribute("employeeList", employeeList);
			jsonUtil.sendJson(response, model);

		}
		// 分配订单
		@RequestMapping(value = "/market/mobile_AllocateOrderSubmit.do", method = RequestMethod.POST)
		public void allocateOrderSubmit(HttpServletRequest request,
				HttpServletResponse response, ModelMap model) {
			int employeeId = Integer.parseInt(request.getParameter("nextEmployeeId"));
			int order_id =  Integer.parseInt(request.getParameter("order_id"));
			Order order = orderService.getOrderById(order_id);
			order.setEmployeeId(employeeId);
			order.setOrderState("A");
			marketService.assignCustomerOrder(order);
			model.addAttribute(Constants.JSON_IS_SUCCESS, true);
			jsonUtil.sendJson(response, model);
		}
		
		//搜索 需要分配的订单
		@RequestMapping(value = "/market/mobile_allocateOrderSearch.do")
		public void allocateOrderSearch(HttpServletRequest request,
				HttpServletResponse response, ModelMap model) {

			String ordernumber = request.getParameter("ordernumber");
			String customername = request.getParameter("customername");
			String stylename = request.getParameter("stylename");
			String employeename = request.getParameter("employeename");
			String startdate = request.getParameter("startdate");
			String enddate = request.getParameter("enddate");
			/*
			 * int j=0; SearchInfo info = null;
			 */
			SearchInfo searchInfo = getSearchInfo(ordernumber, customername,
					stylename, startdate, enddate, employeename);
			// 将用户输入的employeeName转化为employeeId,因为order表中没有employeeName属性
			List<Employee> employees = employeeService
					.getEmployeeByName(employeename);
			Integer[] employeeIds = new Integer[employees.size()];
			for (int i = 0; i < employeeIds.length; i++) {
				employeeIds[i] = employees.get(i).getEmployeeId();
			}
			/*
			 * SearchInfo info = new SearchInfo();
			 * info.setCustomername(customername);
			 * info.setEmployeename(employeename); info.setEnddate(enddate);
			 * info.setOrdernumber(ordernumber); info.setStartdate(startdate);
			 * info.setStylename(stylename);
			 * System.out.println("--------"+info.getStylename());
			 */
			List<Map<String, Object>> list = marketService.getSearchTodoOrderList(ordernumber, customername, stylename, startdate, enddate);

			//修改界面无专员和无进度问题
			for (Map<String, Object> a:list){
				
				Order o=(Order) a.get("order");
				if (o.getOrderState().equals("TODO")){			
					o.setOrderProcessStateName("未选定专员");
					Employee employee=new Employee();
					employee.setEmployeeName("无");
					a.put("order", o);
					a.put("employee", employee);
				
				}
				
			}
			model.addAttribute("list", list);
			model.addAttribute("taskName", "订单列表查找");
			model.addAttribute("url", "/market/mobile_allocateOrderDetail.do");
			model.addAttribute("searchurl", "/market/mobile_allocateOrderSearch.do");
			model.addAttribute("info", searchInfo);// 将查询条件传回页面 hcj

			jsonUtil.sendJson(response, model);
		}
		
}