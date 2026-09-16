package co.sena.edu.themis.Resolver.DgsComponent.DgsData;

import co.sena.edu.themis.Business.NoveltyBusiness;
import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Service.CommitteeService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

@DgsComponent
public class CommitteeEventData {

    private final CommitteeService committeeService;
    private final NoveltyBusiness noveltyBusiness;

    @Autowired
    public CommitteeEventData(CommitteeService committeeService, NoveltyBusiness noveltyBusiness) {
        this.committeeService = committeeService;
        this.noveltyBusiness = noveltyBusiness;
    }

    @DgsData(parentType = "CommitteeEvent", field = "novelties")
    public List<Novelty> novelties(DgsDataFetchingEnvironment env) {
        CommitteeEventDto event = env.getSource();
        if (event == null || event.getCommittee() == null || event.getCommittee().getId() == null) {
            return Collections.emptyList();
        }
        Long committeeId = event.getCommittee().getId();
        Committee committee = committeeService.getById(committeeId);
        if (committee == null || committee.getStudentsIds() == null || committee.getStudentsIds().isEmpty()) {
            return Collections.emptyList();
        }
        return noveltyBusiness.findAllByStudentIds(committee.getStudentsIds());
    }
}

