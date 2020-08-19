package us.codecraft.tinyioc.aop;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

import com.alibaba.fastjson.JSON;

/**
 * @author yihua.huang@dianping.com
 */
public class Cglib2AopProxy extends AbstractAopProxy {

	public Cglib2AopProxy(AdvisedSupport advised) {
		super(advised);
	}

	//通过cglib类库创建了一个代理类的实例
	@Override
	public Object getProxy() {
		//Enhancer 是一个字节码增强器，可以用来为无接口的类创建代理。它的功能与java自带的Proxy类挺相似的。它会根据某个给定的类创建子类，并且所有非final的方法都带有回调钩子
		System.out.println("通过cglib类库创建了一个代理类的实例--动态代理----本质上它是通过动态的生成一个子类去覆盖所要代理的类（非final修饰的类和方法）");
		Enhancer enhancer = new Enhancer();
		System.out.println("设置代理目标="+advised.getTargetSource().getTargetClass());
		enhancer.setSuperclass(advised.getTargetSource().getTargetClass());
		System.out.println("设置代理类的接口="+JSON.toJSONString(advised.getTargetSource().getInterfaces()));
		enhancer.setInterfaces(advised.getTargetSource().getInterfaces());
		//设置代理类的通知方法，相当于设置拦截器方法
		System.out.println("设置enhancer的回调对象");
		enhancer.setCallback(new DynamicAdvisedInterceptor(advised));
		Object enhanced = enhancer.create();
		return enhanced;
	}

	//方法拦截器
	private static class DynamicAdvisedInterceptor implements MethodInterceptor {

		private AdvisedSupport advised;

		private org.aopalliance.intercept.MethodInterceptor delegateMethodInterceptor;

		private DynamicAdvisedInterceptor(AdvisedSupport advised) {
			this.advised = advised;
			this.delegateMethodInterceptor = advised.getMethodInterceptor();
		}

		//调用代理类的方法（代理类与原始类是父子关系，还有一种是兄弟关系，调用实质是调用原始类的方法）
		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			if (advised.getMethodMatcher() == null
					|| advised.getMethodMatcher().matches(method, advised.getTargetSource().getTargetClass())) {
				//这里也应该是先调用拦截方法，然后调用原始对象的方法
				return delegateMethodInterceptor.invoke(new CglibMethodInvocation(advised.getTargetSource().getTarget(), method, args, proxy));
			}
			return new CglibMethodInvocation(advised.getTargetSource().getTarget(), method, args, proxy).proceed();
		}
	}

	private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

		private final MethodProxy methodProxy;

		public CglibMethodInvocation(Object target, Method method, Object[] args, MethodProxy methodProxy) {
			super(target, method, args);
			this.methodProxy = methodProxy;
		}

		@Override
		public Object proceed() throws Throwable {
			return this.methodProxy.invoke(this.target, this.arguments);
		}
	}

}
