package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.ProcessFlowStatus;
import co.sena.edu.themis.Repository.ProcessFlowStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessFlowStatusService {

    @Autowired
    private ProcessFlowStatusRepository processFlowStatusRepository;

    public ProcessFlowStatus findByName(String name) {
        Optional<ProcessFlowStatus> status = processFlowStatusRepository.findByName(name);
        return status.orElseGet(() -> {
            // Si no existe, crear uno nuevo
            ProcessFlowStatus newStatus = new ProcessFlowStatus();
            newStatus.setName(name);
            newStatus.setDescription("Flujo de proceso: " + name);
            return processFlowStatusRepository.save(newStatus);
        });
    }

    public ProcessFlowStatus save(ProcessFlowStatus processFlowStatus) {
        return processFlowStatusRepository.save(processFlowStatus);
    }
}
