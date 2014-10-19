module.exports = function(grunt) {
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),


        concat: {
            css: {
                src: ['css/*'],
                dest: 'css/combined.css'
            }},
        cssmin: {
            css:{
                src: 'css/combined.css',
                dest: 'css/combined.min.css'
            }
        },
        watch: {
            files: ['css/app.css', 'css/om-inputs.css'],
            tasks: ['concat', 'cssmin']
        }

    });

    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.registerTask('default', [ 'concat:css', 'cssmin:css']);
};
