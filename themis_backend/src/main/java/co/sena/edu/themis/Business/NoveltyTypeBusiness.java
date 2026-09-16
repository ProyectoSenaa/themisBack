
package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.themis.Entity.NoveltyType;
import co.sena.edu.themis.Service.NoveltyTypeService;
import co.sena.edu.themis.Utils.Exception.CustomException;
import jakarta.persistence.EntityNotFoundException;
import org.apache.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class NoveltyTypeBusiness {

    private final NoveltyTypeService noveltyTypeService;


    public NoveltyTypeBusiness(NoveltyTypeService noveltyTypeService) {
        this.noveltyTypeService = noveltyTypeService;
    }

    private final ModelMapper modelMapper = new ModelMapper();
    private static final Logger logger = Logger.getLogger(NoveltyTypeBusiness.class);

    private void ValidationObject(NoveltyTypeDto noveltyTypeDto) {
        NoveltyTypeDto existingNoveltyTypeDto = null;
        boolean isUpdate = noveltyTypeDto.getId() != null;
        if (isUpdate) {
            existingNoveltyTypeDto = findById(noveltyTypeDto.getId());
        }
        if (!isUpdate || !noveltyTypeDto.getNameNovelty().equals(existingNoveltyTypeDto.getNameNovelty())) {
            if (noveltyTypeService.existsNameNovelty(noveltyTypeDto.getNameNovelty())) {
                throw new CustomException("Conflict", "The novelty type already exists", HttpStatus.CONFLICT);
            }
        }
    }

    public Page<NoveltyTypeDto> findAll(int page, int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<NoveltyType> noveltyTypes = noveltyTypeService.findAll(pageRequest);

            if (noveltyTypes.isEmpty()) {
                return Page.empty();
            }
            return noveltyTypes.map(NoveltyType -> modelMapper.map(NoveltyType, NoveltyTypeDto.class));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error getting novelties types " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public NoveltyTypeDto findById(Long id) {
        try {
            NoveltyType noveltyType = noveltyTypeService.getById(id);
            logger.info("Novelty type: {}" + noveltyType);
            if (noveltyType != null) {
                return modelMapper.map(noveltyType, NoveltyTypeDto.class);
            } else  {
                throw new CustomException("Not Found", "Not found novelty type with that id", HttpStatus.NOT_FOUND);
            }
        } catch (EntityNotFoundException entNotFound) {
            logger.info(entNotFound.getMessage());
            throw new CustomException("Not Found", "Not found novelty type with that id", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error finding novelty type", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public NoveltyTypeDto createNoveltyType(NoveltyTypeDto noveltyTypeDto) {
        try {
            ValidationObject(noveltyTypeDto);
            NoveltyType noveltyType = modelMapper.map(noveltyTypeDto, NoveltyType.class);
            return modelMapper.map(noveltyTypeService.save(noveltyType), NoveltyTypeDto.class);

        } catch (Exception e) {
            throw new CustomException("Error", "Error creating novelty type " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void updateNoveltyType(NoveltyTypeDto noveltyTypeDto) {
        try {

            if (noveltyTypeDto.getId() == null) {
                logger.info("Can't update novelty type because the id is null!");
            }

            ValidationObject(noveltyTypeDto);

            NoveltyType existingNoveltyType = noveltyTypeService.getById(noveltyTypeDto.getId());


            modelMapper.map(noveltyTypeDto,existingNoveltyType);
            noveltyTypeService.save(existingNoveltyType);

        } catch (EntityNotFoundException entNotFound) {
            logger.info("The novelty type you are trying update is not registered");
            throw new CustomException("Not Found", "Can't update the novelty type because it isn't registered", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error update novelty type", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    public boolean updateStateNoveltyType(NoveltyTypeDto noveltyTypeDto) {
//        try {
//            if (noveltyTypeDto.getId() == null) {
//                logger.info("Can't update state to novelty type because the id is null");
//            }
//
//            NoveltyType existingNoveltyType = noveltyTypeService.getById(noveltyTypeDto.getId());
//            logger.info("Novelty type to update state: " + existingNoveltyType);
//
//            existingNoveltyType.setNoveltyState(noveltyTypeDto.isNoveltyState());
//            noveltyTypeService.save(existingNoveltyType);
//            return true;
//        } catch (EntityNotFoundException entNotFound) {
//            logger.info("The novelty type you are trying to update the state is not registered");
//            throw new CustomException("Not Found", "Can't update the state for novelty type because it isn't registered", HttpStatus.NOT_FOUND);
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//            throw new CustomException("Error", "Error updating the state of the novelty type", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    public void deleteNoveltyTypeById(Long id) {
        try {
            if (id == null) {
                logger.info("Can't delete novelty type because the id is null!");
            }

            NoveltyType deletingNoveltyType = noveltyTypeService.getById(id);
            logger.info("Novelty type: {}" + deletingNoveltyType);

            noveltyTypeService.deleteById(id);
            logger.info("Novelty type deleted successfully");
        } catch (EntityNotFoundException entNotFound) {
            logger.info("The novelty type you are trying delete is not registered");
            throw new CustomException("Not Found", "Can't delete the novelty type because it isn't registered", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error delete novelty type", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
