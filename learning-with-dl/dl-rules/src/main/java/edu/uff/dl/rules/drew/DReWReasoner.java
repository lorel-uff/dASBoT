/*
 * UFF Project Semantic Learning
 */
package edu.uff.dl.rules.drew;

import edu.uff.dl.rules.rules.AnswerRule;
import edu.uff.dl.rules.rules.AnswerSetRule;
import edu.uff.dl.rules.datalog.ConcreteLiteral;
import edu.uff.dl.rules.datalog.DataLogPredicate;
import edu.uff.dl.rules.expansion.set.ExpansionAnswerSet;
import edu.uff.dl.rules.expansion.set.SampleExpansionAnswerSet;
import edu.uff.dl.rules.datalog.SimplePredicate;
import edu.uff.dl.rules.expansion.set.IndividualTemplate;
import edu.uff.dl.rules.expansion.set.parallel.ParallelExpansionAnswerSet;
import edu.uff.dl.rules.expansion.set.parallel.ParallelSampleExpansionAnswerSet;
import edu.uff.dl.rules.test.App;
import edu.uff.dl.rules.util.FileContent;
import it.unical.mat.wrapper.DLVInvocationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dllearner.core.Component;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.semanticweb.drew.dlprogram.model.Clause;
import org.semanticweb.drew.dlprogram.model.Constant;
import org.semanticweb.drew.dlprogram.model.DLProgram;
import org.semanticweb.drew.dlprogram.model.DLProgramKB;
import org.semanticweb.drew.dlprogram.model.Literal;
import org.semanticweb.drew.dlprogram.model.NormalPredicate;
import org.semanticweb.drew.dlprogram.model.Predicate;
import org.semanticweb.drew.dlprogram.model.ProgramStatement;
import org.semanticweb.drew.dlprogram.model.Term;
import org.semanticweb.drew.dlprogram.parser.DLProgramParser;
import org.semanticweb.drew.dlprogram.parser.ParseException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 *
 * @author Victor
 */
@ComponentAnn(name = "DReWReasoner", shortName = "drewreas", version = 0.1)
public class DReWReasoner implements Component {

    protected DReWRLCLILiteral drew;
    protected OWLOntology ontology;
    protected Set<Constant> individuals;
    protected Set<DataLogPredicate> predicates;
    protected Set<Literal> samples;
    protected String dlpContent;
    protected String samplesContent;
    protected List<AnswerSetRule> answerSetRules;
    protected String templateContent;
    protected int offSet = 0;

    private String[] arg = {
        "-rl",
        "-ontology",
        "",
        "-dlp",
        "",
        "-dlv",
        "/usr/lib/dlv.i386-apple-darwin-iodbc.bin"
    };

    public DReWReasoner() {
        this.individuals = new HashSet<>();
        this.predicates = new HashSet<>();
        this.samples = new HashSet<>();
        answerSetRules = new ArrayList<>();
    }

    public DReWReasoner(String owlFilePath, String dlpContent, String samplesContent, String templateContent) {
        this();
        this.arg[2] = owlFilePath;
        this.dlpContent = dlpContent;
        this.samplesContent = samplesContent;
        this.templateContent = templateContent;
    }

    @Override
    public void init() throws ComponentInitException {
        try {
            loadIndividualsAndPredicates(individuals, predicates);
            
            //loadSamples(individuals, predicates);
            drew = DReWRLCLILiteral.get(arg);
            drew.setDLPContent(getDlpAndSamples());
            drew.go();
            
            AnswerSetRule aes;
            //loadFromAnswerSet(individuals, predicates);
            ExpansionAnswerSet e;
            for (Set<Literal> answerSet : drew.getLiteralModelHandler().getAnswerSets()) {
                System.out.println("Iniciar Configuração do Template: " + App.getTime());
                //SampleExpansionAnswerSet s = new SampleExpansionAnswerSet(answerSet, samples, individuals, predicates);
                IndividualTemplate it = new IndividualTemplate(templateContent, dlpContent + samplesContent, FileContent.getStringFromFile(getOwlFilePath()));
                it.init();
                System.out.println("Iniciar Geração do Conjunto Expandido: " + App.getTime());
                e = new SampleExpansionAnswerSet(answerSet, samples, it);
                ((SampleExpansionAnswerSet) e).setOffSet(offSet);
                System.out.println("");
                System.out.println(e.getClass());
                System.out.println("");
                //e = s;
                //System.out.println(e.getSamples());
                
                e.init();
                //System.out.println(e);
                //System.out.println("");
                System.out.println("Iniciar Geração da Regra: " + App.getTime());
                System.out.println("");
                int deep = 1;
                System.out.println("Gerando regra com profundidade de variáveis: " + deep);
                AnswerRule ar = new AnswerRule(e.getSamples(), e.getExpansionSet(), deep);
                ar.init();
                aes = new AnswerSetRule(e, ar);
                answerSetRules.add(aes);
            }

        } catch (ParseException | ComponentInitException | FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            throw new ComponentInitException(ex.getMessage());
        }
    }

    public void loadIndividualsAndPredicates(Set<Constant> individuals, Set<DataLogPredicate> predicates) throws FileNotFoundException, ParseException {
        loadOntology(individuals, predicates);
        loadDLP(individuals, predicates);
        loadSamples(individuals, predicates);
    }

    private void loadOntology(Set<Constant> individuals, Set<DataLogPredicate> predicates) {
        if (individuals == null || predicates == null)
            return;

        File file = new File(getOwlFilePath());
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        try {
            ontology = man.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        Constant c;
        Set<OWLNamedIndividual> individualsInSignature = ontology.getIndividualsInSignature();
        for (OWLNamedIndividual o : individualsInSignature) {
            c = new Constant(o.toString());
            individuals.add(c);
        }

        SimplePredicate sp;
        Set<OWLClass> classInSingnature = ontology.getClassesInSignature();
        for (OWLClass o : classInSingnature) {
            sp = new SimplePredicate(o.toString(), 1);
            predicates.add(sp);
        }

        Set<OWLObjectProperty> objectProperties = ontology.getObjectPropertiesInSignature();
        for (OWLObjectProperty o : objectProperties) {
            sp = new SimplePredicate(o.toString(), 2);
            predicates.add(sp);
        }

        predicates.remove(new SimplePredicate("owl:Thing", 1));
    }

    private void loadDLP(Set<Constant> individuals, Set<DataLogPredicate> predicates) throws FileNotFoundException, ParseException {
        List<ProgramStatement> programs = getProgramStatements(dlpContent);
        Clause c;
        SimplePredicate sp;
        Predicate p;
        NormalPredicate np;

        for (ProgramStatement ps : programs) {
            if (ps.isClause() && (c = ps.asClause()).isFact()) {

                p = c.getHead().getPredicate();
                if (p instanceof NormalPredicate) {
                    np = (NormalPredicate) p;

                    sp = new SimplePredicate(np);
                    predicates.add(sp);

                    for (Term term : c.getHead().getTerms()) {
                        if (term instanceof Constant) {
                            individuals.add((Constant) term);
                        }
                    }
                }

            }
        }

    }

    private void loadSamples(Set<Constant> individuals, Set<DataLogPredicate> predicates) throws ParseException {
        loadSamplesLiterals();
        Predicate p;
        NormalPredicate np;
        SimplePredicate sp;
        for (Literal l : samples) {
            p = l.getPredicate();
            if (p instanceof NormalPredicate) {
                np = (NormalPredicate) p;

                sp = new SimplePredicate(np);
                predicates.add(sp);
                for (Term term : l.getTerms()) {
                    if (term instanceof Constant) {
                        individuals.add((Constant) term);
                    }
                }
            }
        }
    }

    private void loadFromAnswerSet(Set<Constant> individuals, Set<DataLogPredicate> predicates) {

        Predicate p;
        NormalPredicate np;
        SimplePredicate sp;

        for (Set<Literal> set : drew.getLiteralModelHandler().getAnswerSets()) {
            for (Literal l : set) {
                p = l.getPredicate();
                if (p instanceof NormalPredicate) {
                    np = (NormalPredicate) p;

                    sp = new SimplePredicate(np);
                    predicates.add(sp);
                    for (Term term : l.getTerms()) {
                        if (term instanceof Constant) {
                            individuals.add((Constant) term);
                        }
                    }
                }
            }
        }

    }

    private void loadSamplesLiterals() throws ParseException {
        List<ProgramStatement> programs = getProgramStatements(samplesContent);
        Clause c;
        Literal l;

        for (ProgramStatement ps : programs) {
            if (ps.isClause() && (c = ps.asClause()).isFact()) {
                l = new Literal(c.getHead().getPredicate(), c.getHead().getTerms());
                samples.add(l);
            }
        }
    }

    private List<ProgramStatement> getProgramStatements(String content) throws ParseException {
        DLProgramKB kb = new DLProgramKB();
        kb.setOntology(ontology);
        DLProgram elprogram = null;

        DLProgramParser parser;

        Reader reader;

        reader = new StringReader(content);

        parser = new DLProgramParser(reader);

        if (ontology != null)
            parser.setOntology(ontology);
        elprogram = parser.program();
        kb.setProgram(elprogram);
        return elprogram.getStatements();
    }

    private String getDlpAndSamples() {
        return dlpContent + samplesContent;
    }

    public void setDlpContent(String dlpContent) {
        this.dlpContent = dlpContent;
    }

    public void setSamplesContent(String samplesContent) {
        this.samplesContent = samplesContent;
    }

    public Set<Constant> getIndividuals() {
        return individuals;
    }

    public Set<DataLogPredicate> getPredicates() {
        return predicates;
    }

    public Set<Literal> getSamples() {
        return samples;
    }

    public String getDlpContent() {
        return dlpContent;
    }

    public String getSamplesContent() {
        return samplesContent;
    }

    public List<AnswerSetRule> getAnswerSetRules() {
        return answerSetRules;
    }

    public String getOwlFilePath() {
        return arg[2];
    }

    public void setOwlFilePath(String owlFilePath) {
        arg[2] = owlFilePath;
    }

    public String[] getArg() {
        return arg;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public int getOffSet() {
        return offSet;
    }

    public void setOffSet(int offSet) {
        this.offSet = offSet;
    }
    
    public void killDLV() throws DLVInvocationException {
        if (drew != null) {
            drew.killDLV();
        }
    }
}
