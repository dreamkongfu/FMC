package nju.software.dataobject;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Produce entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "produce", catalog = "fmc")
public class Produce implements java.io.Serializable {

	// Fields

	private Integer pid;
	private Integer oid;
	private String color;
	private Integer xs;
	private Integer s;
	private Integer m;
	private Integer l;
	private Integer xl;
	private Integer xxl;
	private Integer j;
	private Integer produceAmount;
	private String type;
	
	private String sendTime;	//外发时间
	private String Purchase_director;//负责人
	private String processing_side;//加工方
	
	public static final String TYPE_SAMPLE_PRODUCE = "sampleProduce";//生产样衣所需件数
	public static final String TYPE_SAMPLE_PRODUCED = "sampleProduced";//实际生产件数
	public static final String TYPE_PRODUCE="PRODUCE";
	public static final String TYPE_PRODUCED="PRODUCED";
	public static final String TYPE_QUALIFIED="QUALIFIED";
	public static final String TYPE_UNQUALIFIED="UNQUALIFIED";
	

	// Constructors

	/** default constructor */
	public Produce() {
	}

	/** minimal constructor */
	public Produce(Integer pid) {
		this.pid = pid;
	}

	/** full constructor */
	public Produce(Integer pid, Integer oid, String color, Integer xs,
			Integer s, Integer m, Integer l, Integer xl, Integer xxl,
			 Integer j,String type) {
		this.pid = pid;
		this.oid = oid;
		this.color = color;
		this.xs = xs;
		this.s = s;
		this.m = m;
		this.l = l;
		this.xl = xl;
		this.xxl = xxl;
		this.j = j;
		this.type = type;
	}

	// Property accessors
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "pid", unique = true, nullable = false)
	public Integer getPid() {
		return this.pid;
	}

	public void setPid(Integer pid) {
		this.pid = pid;
	}

	@Column(name = "oid")
	public Integer getOid() {
		return this.oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	@Column(name = "color", length = 45)
	public String getColor() {
		return this.color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Column(name = "XS")
	public Integer getXs() {
		return this.xs;
	}

	public void setXs(Integer xs) {
		this.xs = xs;
	}

	@Column(name = "S")
	public Integer getS() {
		return this.s;
	}

	public void setS(Integer s) {
		this.s = s;
	}

	@Column(name = "M")
	public Integer getM() {
		return this.m;
	}

	public void setM(Integer m) {
		this.m = m;
	}

	@Column(name = "L")
	public Integer getL() {
		return this.l;
	}

	public void setL(Integer l) {
		this.l = l;
	}

	@Column(name = "XL")
	public Integer getXl() {
		return this.xl;
	}

	@Column(name = "sendTime")
	public String getSendTime() {
		return sendTime;
	}

	public void setSendTime(String sendTime) {
		this.sendTime = sendTime;
	}

	@Column(name = "Purchase_director")
	public String getPurchase_director() {
		return Purchase_director;
	}

	public void setPurchase_director(String purchase_director) {
		Purchase_director = purchase_director;
	}
	
	@Column(name = "processing_side")
	public String getProcessing_side() {
		return processing_side;
	}

	public void setProcessing_side(String processing_side) {
		this.processing_side = processing_side;
	}
	
	public void setXl(Integer xl) {
		this.xl = xl;
	}

	@Column(name = "XXL")
	public Integer getXxl() {
		return this.xxl;
	}

	public void setXxl(Integer xxl) {
		this.xxl = xxl;
	}

	@Column(name = "J")
	public Integer getJ() {
		return this.j;
	}
	
	public void setJ(Integer j) {
		this.j = j;
	}
	
	@Column(name = "type", length = 45)
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "produceAmount")
	public Integer getProduceAmount() {
		return produceAmount;
	}

	public void setProduceAmount(Integer produceAmount) {
		this.produceAmount = produceAmount;
	}

}