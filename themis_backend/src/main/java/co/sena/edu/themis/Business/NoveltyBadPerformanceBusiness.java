package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Strategy.FollowUpValidationContext;
import co.sena.edu.themis.Service.FollowUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltyBadPerformanceBusiness {

    @Autowired
    private FollowUpValidationContext validationContext;

    @Autowired
    private FollowUpService followUpService;

    public FollowUp processFollowUp(FollowUp followUp) {
        FollowUp processedFollowUp = validationContext.validateAndProcess(followUp);
        return followUpService.save(processedFollowUp);
    }

    public boolean validateFicha(FollowUp followUp) {
        try {
            FollowUp validatedFollowUp = validationContext.validateAndProcess(followUp);
            return !validatedFollowUp.getFollowUpStatus().getName().equals("rechazado");
        } catch (Exception e) {
            return false;
        }
    }
}
