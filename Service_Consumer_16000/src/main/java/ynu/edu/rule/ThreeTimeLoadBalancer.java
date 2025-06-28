package ynu.edu.rule;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;

public class ThreeTimeLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private int instance_call_count = 0; //已经被调用的次数
    private int instance_index = 0; //当前实例的索引
    private final String serviceId; //服务ID
    private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSuppliers; //服务实例列表供应商

    public ThreeTimeLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSuppliers,
                                 String serviceId) {
        this.serviceInstanceListSuppliers = serviceInstanceListSuppliers;
        this.serviceId = serviceId;
    }


    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = this.serviceInstanceListSuppliers.getIfAvailable();
//        首先判断当前实例的调用次数是否已经达到三次，如果达到三次，则返回null，否则，获取服务实例列表，并选择其中一个实例，并返回。
//        当你做选择的时候，请你获取目前可以用的服务实例列表，然后选择其中一个实例，并返回。
        return supplier.get().next().map(this::getInstanceResponse);
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
//        先判断有没有内容
        if (instances.isEmpty()) {
            return new EmptyResponse();
        }
        int size = instances.size();
        // 已经被调用的次数
        ServiceInstance serviceInstance = null;
        while (serviceInstance == null) {
            if (instance_call_count < 3) {
                // 选择当前实例
                serviceInstance = instances.get(this.instance_index);
                instance_call_count++;
            } else {
                this.instance_index++;
                this.instance_call_count = 0;
                if (this.instance_index >= size) {
                    this.instance_index = 0;
                }
            }
        }
            return new DefaultResponse(serviceInstance);
    }
}