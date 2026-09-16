package co.sena.edu.themis.Utils.config.Seeders;

import co.sena.edu.themis.Entity.*;
import co.sena.edu.themis.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigSeeders implements CommandLineRunner {

    @Autowired
    private NoveltyTypeRepository noveltyTypeRepository;

    @Autowired
    private NoveltyStatusRepository noveltyStatusRepository;

    @Autowired
    private ProcessFlowStatusRepository processFlowStatusRepository;

    @Autowired
    private FollowUpTypeRepository followUpTypeRepository;

    @Autowired
    private FollowUpStatusRepository followUpStatusRepository;

    @Autowired
    private FollowUpFlowStatusRepository followUpFlowStatusRepository;

    @Override
    public void run(String... args) throws Exception {
        // Puedes llamar a tus métodos de seeder aquí
        createNoveltyTypes();
        createNoveltyStatuses();
        createProcessFlowStatuses();

        // Seeders para módulo de seguimientos académicos
        createFollowUpTypes();
        createFollowUpStatuses();
        createFollowUpFlowStatuses();
    }

    /**
     * Crea los tipos de novedad con referencias al rol SuperAdmin (ID 17) de la API externa
     */
    private void createNoveltyTypes() {
        // Verificar si ya existen tipos de novedad para evitar duplicados
        if (noveltyTypeRepository.count() > 0) {
            System.out.println("Novelty types already exist, skipping seeding...");
            return;
        }

        System.out.println("Creating novelty types...");

        Long superAdminRoleId = 1L;

        // Lista para almacenar todos los tipos de novedad que crearemos
        List<NoveltyType> noveltyTypes = new ArrayList<>();

        // ===== APLAZAMIENTO =====
        NoveltyType aplazamiento = new NoveltyType();
        aplazamiento.setNameNovelty("Aplazamiento");
        aplazamiento.setActive(true);
        aplazamiento.setDescription("Se puede tramitar por motivos personales o por incapacidades mayores a 20 días.");
        aplazamiento.setProcedureDescription("Presentar solicitud con documentación de soporte.");
        aplazamiento.setNoveltyList(new ArrayList<>());


        noveltyTypes.add(aplazamiento);

        // ===== CANCELACIÓN DE MATRÍCULA =====
        NoveltyType cancelacionMatricula = new NoveltyType();
        cancelacionMatricula.setNameNovelty("Cancelación de Matrícula");
        cancelacionMatricula.setActive(true);
        cancelacionMatricula.setDescription("Se puede tramitar en cualquier momento.");
        cancelacionMatricula.setProcedureDescription("Presentar solicitud formal de cancelación.");
        cancelacionMatricula.setNoveltyList(new ArrayList<>());

        noveltyTypes.add(cancelacionMatricula);

        // ===== CONDICIONAMIENTO DE MATRÍCULA =====
        NoveltyType condicionamientoMatricula = new NoveltyType();
        condicionamientoMatricula.setNameNovelty("Condicionamiento de Matrícula");
        condicionamientoMatricula.setActive(true);
        condicionamientoMatricula.setDescription("Se puede tramitar en cualquier momento.");
        condicionamientoMatricula.setProcedureDescription("Evaluación de desempeño y comportamiento del aprendiz.");
        condicionamientoMatricula.setNoveltyList(new ArrayList<>());

        noveltyTypes.add(condicionamientoMatricula);

        // ===== DESERCIÓN =====
        NoveltyType desercion = new NoveltyType();
        desercion.setNameNovelty("Deserción");
        desercion.setActive(true);
        desercion.setDescription("Se puede tramitar en cualquier momento.");
        desercion.setProcedureDescription("Registro de inasistencia prolongada sin justificación.");
        desercion.setNoveltyList(new ArrayList<>());

        noveltyTypes.add(desercion);

        // ===== REINGRESO =====
        NoveltyType reingreso = new NoveltyType();
        reingreso.setNameNovelty("Reingreso");
        reingreso.setActive(true);
        reingreso.setDescription("Se puede tramitar en cualquier momento.");
        reingreso.setProcedureDescription("Presentar solicitud de reingreso con justificación.");
        reingreso.setNoveltyList(new ArrayList<>());

        noveltyTypes.add(reingreso);

        // ===== RETIRO VOLUNTARIO =====
        NoveltyType retiroVoluntario = new NoveltyType();
        retiroVoluntario.setNameNovelty("Retiro Voluntario");
        retiroVoluntario.setActive(true);
        retiroVoluntario.setDescription("Se puede tramitar en cualquier momento.");
        retiroVoluntario.setProcedureDescription("Presentar carta de retiro voluntario.");
        retiroVoluntario.setNoveltyList(new ArrayList<>());

        noveltyTypes.add(retiroVoluntario);

        // ===== TRASLADO =====
        NoveltyType traslado = new NoveltyType();
        traslado.setNameNovelty("Traslado");
        traslado.setActive(true);
        traslado.setDescription("Se puede solicitar una única vez y se recomienda que sea en el inicio o final de un trimestre.");
        traslado.setProcedureDescription("Presentar solicitud con justificación y documentación de soporte.");
        traslado.setNoveltyList(new ArrayList<>());

        noveltyTypes.add(traslado);

        // ===== GUARDAR TODOS LOS TIPOS DE NOVEDAD =====
        noveltyTypeRepository.saveAll(noveltyTypes);

        System.out.println("Novelty types created successfully");
    }

    private void createNoveltyStatuses() {
        if (noveltyStatusRepository.count() > 0) {
            System.out.println("Novelty statuses already exist, skipping seeding...");
            return;
        }

        System.out.println("Creating novelty statuses...");
        List<NoveltyStatus> statuses = new ArrayList<>();

        NoveltyStatus pendiente = new NoveltyStatus();
        pendiente.setName("pendiente");
        pendiente.setDescription("Aún no la ha revisado el coordinador");
        statuses.add(pendiente);

        NoveltyStatus enProceso = new NoveltyStatus();
        enProceso.setName("en proceso");
        enProceso.setDescription("Ya se está revisando para saber si va a comité o se aprueba o deniega directamente");
        statuses.add(enProceso);

        NoveltyStatus aprobado = new NoveltyStatus();
        aprobado.setName("aprobado");
        aprobado.setDescription("Novedad aprobada");
        statuses.add(aprobado);

        NoveltyStatus denegado = new NoveltyStatus();
        denegado.setName("denegado");
        denegado.setDescription("Se confirma que tiene que volver a realizar la solicitud actualizando y modificando la novedad por algún error");
        statuses.add(denegado);

        NoveltyStatus noAprobado = new NoveltyStatus();
        noAprobado.setName("no aprobado");
        noAprobado.setDescription("Novedad no aprobada");
        statuses.add(noAprobado);

        noveltyStatusRepository.saveAll(statuses);
        System.out.println("Novelty statuses created successfully");
    }

    // Nuevo: seeding de estados de flujo de proceso
    private void createProcessFlowStatuses() {
        System.out.println("Ensuring process flow statuses exist...");
        List<ProcessFlowStatus> toCreate = new ArrayList<>();

        if (!processFlowStatusRepository.existsByName("pendiente")) {
            ProcessFlowStatus pending = new ProcessFlowStatus();
            pending.setName("pendiente");
            pending.setDescription("Flujo pendiente (cuando la novedad está en pendiente)");
            toCreate.add(pending);
        }

        if (!processFlowStatusRepository.existsByName("coordinacion")) {
            ProcessFlowStatus coordination = new ProcessFlowStatus();
            coordination.setName("coordinacion");
            coordination.setDescription("Flujo de coordinación (cuando la novedad cambia a estados intermedios)");
            toCreate.add(coordination);
        }

        if (!processFlowStatusRepository.existsByName("comite")) {
            ProcessFlowStatus committee = new ProcessFlowStatus();
            committee.setName("comite");
            committee.setDescription("Flujo de comité (cuando pasa o se registra en comité)");
            toCreate.add(committee);
        }

        if (!toCreate.isEmpty()) {
            processFlowStatusRepository.saveAll(toCreate);
            System.out.println("Process flow statuses created: " + toCreate.size());
        } else {
            System.out.println("Process flow statuses already present. Skipping creation.");
        }
    }

    private void createFollowUpTypes() {
        // Verificar si ya existen tipos de seguimiento para evitar duplicados
        if (followUpTypeRepository.count() > 0) {
            System.out.println("Follow-up types already exist, skipping seeding...");
            return;
        }

        System.out.println("Creating follow-up types...");

        // Lista para almacenar todos los tipos de seguimiento que crearemos
        List<FollowUpType> followUpTypes = new ArrayList<>();

        // ===== BAJO RENDIMIENTO ACADÉMICO =====
        FollowUpType bajoRendimiento = new FollowUpType();
        bajoRendimiento.setName("Bajo Rendimiento Académico");
        bajoRendimiento.setActive(true);
        bajoRendimiento.setDescription("Seguimiento para aprendices con calificaciones deficientes o dificultades en el proceso de aprendizaje que requieren intervención y plan de mejoramiento.");
        bajoRendimiento.setFollowUpList(new ArrayList<>());
        followUpTypes.add(bajoRendimiento);

        // ===== FALTAS GRAVES =====
        FollowUpType faltasGraves = new FollowUpType();
        faltasGraves.setName("Faltas Graves");
        faltasGraves.setActive(true);
        faltasGraves.setDescription("Seguimiento para aprendices que han incurrido en faltas disciplinarias graves según el reglamento estudiantil que requieren intervención inmediata.");
        faltasGraves.setFollowUpList(new ArrayList<>());
        followUpTypes.add(faltasGraves);

        // ===== GUARDAR TODOS LOS TIPOS DE SEGUIMIENTO =====
        followUpTypeRepository.saveAll(followUpTypes);

        System.out.println("Follow-up types created successfully");
    }

    private void createFollowUpStatuses() {
        if (followUpStatusRepository.count() > 0) {
            System.out.println("Follow-up statuses already exist, skipping seeding...");
            return;
        }

        System.out.println("Creating follow-up statuses...");
        List<FollowUpStatus> statuses = new ArrayList<>();

        // ===== ABIERTO (por defecto) =====
        FollowUpStatus abierto = new FollowUpStatus();
        abierto.setName("abierto");
        abierto.setDescription("Caso de seguimiento abierto y activo");
        statuses.add(abierto);

        // ===== EN REVISIÓN =====
        FollowUpStatus enRevision = new FollowUpStatus();
        enRevision.setName("en revision");
        enRevision.setDescription("Caso en proceso de revisión por el coordinador o comité");
        statuses.add(enRevision);

        // ===== RESUELTO =====
        FollowUpStatus resuelto = new FollowUpStatus();
        resuelto.setName("resuelto");
        resuelto.setDescription("Caso resuelto con decisión final del comité");
        statuses.add(resuelto);

        // ===== CONDICIONAMIENTO DE MATRÍCULA =====
        FollowUpStatus condicionamientoMatricula = new FollowUpStatus();
        condicionamientoMatricula.setName("condicionamiento de matricula");
        condicionamientoMatricula.setDescription("Decisión de condicionamiento de matrícula por el comité");
        statuses.add(condicionamientoMatricula);

        // ===== CANCELAMIENTO DE MATRÍCULA =====
        FollowUpStatus cancelamientoMatricula = new FollowUpStatus();
        cancelamientoMatricula.setName("cancelamiento de matricula");
        cancelamientoMatricula.setDescription("Decisión de cancelamiento de matrícula por el comité");
        statuses.add(cancelamientoMatricula);

        // ===== APLAZAMIENTO DEL PROCESO FORMATIVO =====
        FollowUpStatus aplazamientoProceso = new FollowUpStatus();
        aplazamientoProceso.setName("aplazamiento del proceso formativo");
        aplazamientoProceso.setDescription("Decisión de aplazamiento del proceso formativo por el comité");
        statuses.add(aplazamientoProceso);

        // ===== PLAN DE MEJORAMIENTO ADICIONAL =====
        FollowUpStatus planMejoramientoAdicional = new FollowUpStatus();
        planMejoramientoAdicional.setName("plan de mejoramiento adicional");
        planMejoramientoAdicional.setDescription("Se requiere un plan de mejoramiento adicional para el aprendiz");
        statuses.add(planMejoramientoAdicional);

        followUpStatusRepository.saveAll(statuses);
        System.out.println("Follow-up statuses created successfully");
    }

    private void createFollowUpFlowStatuses() {
        System.out.println("Ensuring follow-up flow statuses exist...");
        List<FollowUpFlowStatus> toCreate = new ArrayList<>();

        // ===== COORDINACIÓN (por defecto) =====
        if (!followUpFlowStatusRepository.existsByName("coordinacion")) {
            FollowUpFlowStatus coordinacion = new FollowUpFlowStatus();
            coordinacion.setName("coordinacion");
            coordinacion.setDescription("Flujo en coordinación - estado por defecto para seguimientos académicos");
            toCreate.add(coordinacion);
        }

        // ===== COMITÉ =====
        if (!followUpFlowStatusRepository.existsByName("comite")) {
            FollowUpFlowStatus comite = new FollowUpFlowStatus();
            comite.setName("comite");
            comite.setDescription("Flujo en comité para casos que requieren decisión del comité de evaluación");
            toCreate.add(comite);
        }

        if (!toCreate.isEmpty()) {
            followUpFlowStatusRepository.saveAll(toCreate);
            System.out.println("Follow-up flow statuses created: " + toCreate.size());
        } else {
            System.out.println("Follow-up flow statuses already present. Skipping creation.");
        }
    }
}